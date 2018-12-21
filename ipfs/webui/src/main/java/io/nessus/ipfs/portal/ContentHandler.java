package io.nessus.ipfs.portal;

/*-
 * #%L
 * Nessus :: IPFS :: WebUI
 * %%
 * Copyright (C) 2018 Nessus
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.Network;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.ContentManagerConfig;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.jaxrs.JAXRSClient;
import io.nessus.ipfs.jaxrs.SAHandle;
import io.nessus.ipfs.jaxrs.SFHandle;
import io.nessus.ipfs.portal.TreeData.TreeNode;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;
import io.nessus.utils.StreamUtils;
import io.nessus.utils.SystemUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.util.Headers;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;

public class ContentHandler implements HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ContentHandler.class);

    final Blockchain blockchain;
    final Network network;
    final Wallet wallet;

    final IPFSClient ipfsClient;
    final JAXRSClient jaxrsClient;
    final VelocityEngine ve;
    final URI gatewayUrl;

    // Executor service for async operations
    final ExecutorService executorService;

    // The last executable job
    private Future<Address> lastJob;

    ContentHandler(String appName, WebUIConfig config) throws Exception {
    	
        blockchain = config.getBlockchain();
        network = blockchain.getNetwork();
        wallet = blockchain.getWallet();
        JAXRSClient.logBlogchainNetworkAvailable(blockchain.getNetwork());
        
        String envHost = SystemUtils.getenv(ContentManagerConfig.ENV_IPFS_GATEWAY_ADDR, "127.0.0.1");
        String envPort = SystemUtils.getenv(ContentManagerConfig.ENV_IPFS_GATEWAY_PORT, "8080");
        gatewayUrl = new URI(String.format("http://%s:%s/ipfs", envHost, envPort));

        ipfsClient = config.getIPFSClient();
        LOG.info("IPFS PeerId: {}",  ipfsClient.getPeerId());
        LOG.info("IPFS Gateway: {}", gatewayUrl);
        LOG.info("IPFS Address: {}",  ipfsClient.getAPIAddress());
        LOG.info("IPFS Version: {}",  ipfsClient.version());

        URL jaxrsUrl = config.getJaxrsUrl();
        jaxrsClient = new JAXRSClient(jaxrsUrl);
        LOG.info("Nessus JAXRS: {}", jaxrsUrl);

        LOG.info("{} WebUI: {}", appName, config.getWebUiUrl());
        
        ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();

        executorService = Executors.newFixedThreadPool(1, new ThreadFactory() {
            AtomicInteger count = new AtomicInteger();

            public Thread newThread(Runnable run) {
                return new Thread(run, "webui-pool-" + count.incrementAndGet());
            }
        });
	}

	@Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        ByteBuffer content = null;

        String path = exchange.getRelativePath();
        if (path.startsWith("/portal")) {
            content = dynamicContent(exchange);
        } else {
            content = staticContent(exchange);
        }

        if (content != null) {
            exchange.getResponseSender().send(content);
        }
    }

    private ByteBuffer dynamicContent(HttpServerExchange exchange) throws Exception {

        String relPath = exchange.getRelativePath();

        // Check the status of our last asynch job
        if (lastJob != null) {
            if (lastJob.isDone()) {
                try {
                    Address addr = lastJob.get();
                    LOG.info("Successfully imported: " + addr);
                } finally {
                    lastJob = null;
                }
            } else {
                LOG.info("Last import job still running ...");
            }
        }

        String tmplPath = null;
        VelocityContext context = new VelocityContext();
        context.put("implVersion", WebUI.getImplVersion());
        context.put("implBuild", WebUI.getImplBuild() != null ? WebUI.getImplBuild() : "");

        try {

            // Assert Blockchain availability
        	
            assertBlockchainAvailable(context);

            // Assert IPFS availability
            
            assertIpfsAvailable(context);
            
            if (relPath.startsWith("/portal/addtxt")) {
                actAddIpfsText(exchange, context);
            }

            else if (relPath.startsWith("/portal/addurl")) {
                actAddIpfsURL(exchange, context);
            }

            else if (relPath.startsWith("/portal/addpath")) {
                actAddIpfsPath(exchange, context);
            }

            else if (relPath.startsWith("/portal/assign")) {
                actAssignLabel(exchange, context);
            }

            else if (relPath.startsWith("/portal/fget")) {
                actIpfsGet(exchange, context);
            }

            else if (relPath.startsWith("/portal/fshow")) {
                return actShowLocal(exchange, context);
            }

            else if (relPath.startsWith("/portal/impkey")) {
                actImportKey(exchange, context);
            }

            else if (relPath.startsWith("/portal/newaddr")) {
                actNewAddress(exchange, context);
            }

            else if (relPath.startsWith("/portal/padd")) {
                tmplPath = pageIpfsAdd(exchange, context);
            }

            else if (relPath.startsWith("/portal/pget")) {
                tmplPath = pageIpfsGet(exchange, context);
            }

            else if (relPath.startsWith("/portal/plist")) {
                tmplPath = pageIpfsList(exchange, context);
            }

            else if (relPath.startsWith("/portal/pqr")) {
                tmplPath = pageQRCode(exchange, context);
            }

            else if (relPath.startsWith("/portal/psend")) {
                tmplPath = pageIpfsSend(exchange, context);
            }

            else if (relPath.startsWith("/portal/regaddr")) {
                actRegisterAddress(exchange, context);
            }

            else if (relPath.startsWith("/portal/rmaddr")) {
                actUnregisterAddress(exchange, context);
            }

            else if (relPath.startsWith("/portal/rmlocal")) {
                actRemoveLocal(exchange, context);
            }

            else if (relPath.startsWith("/portal/rmipfs")) {
                actUnregisterIpfs(exchange, context);
            }

            else if (relPath.startsWith("/portal/sendcid")) {
                actIpfsSend(exchange, context);
            }

            else if (tmplPath == null) {
                tmplPath = pageHome(context);
            }

        } catch (Exception ex) {

            LOG.error("Error", ex);
            tmplPath = pageError(context, ex);
        }

        if (tmplPath != null) {

            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/html");

            try (InputStreamReader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(tmplPath))) {

                StringWriter strwr = new StringWriter();
                ve.evaluate(context, strwr, tmplPath, reader);

                return ByteBuffer.wrap(strwr.toString().getBytes());
            }
        }

        return null;
    }

    private void actAssignLabel(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String label = qparams.get("label").getFirst();

        Address addr = wallet.findAddress(rawAddr);
        addr.setLabels(Arrays.asList(label));

        redirectHomePage(exchange);
    }

    private void actImportKey(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String key = qparams.get("impkey").getFirst();
        String label = qparams.get("label").getFirst();

        AssertState.assertTrue(lastJob == null || lastJob.isDone(), "Last import job is not yet done");

        lastJob = executorService.submit(new Callable<Address>() {

            @Override
            public Address call() throws Exception {
                if (wallet.isP2PKH(key)) {
                    LOG.info("Importing watch only address: {}", key);
                    return wallet.importAddress(key, Arrays.asList(label));
                } else {
                    LOG.info("Importing private key: P**************");
                    return wallet.importPrivateKey(key, Arrays.asList(label));
                }
            }
        });

        redirectHomePage(exchange);
    }

    private void actNewAddress(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String label = qparams.get("label").getFirst();

        wallet.newAddress(label);

        redirectHomePage(exchange);
    }

    private void actRegisterAddress(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();

        jaxrsClient.registerAddress(rawAddr);

        redirectHomePage(exchange);
    }

    private void actUnregisterAddress(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();

        jaxrsClient.unregisterAddress(rawAddr);

        redirectFileList(exchange, rawAddr);
    }

    private void actAddIpfsText(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();
        String content = qparams.get("content").getFirst();

        jaxrsClient.addIpfsContent(rawAddr, relPath, new ByteArrayInputStream(content.getBytes()));

        redirectFileList(exchange, rawAddr);
    }

    private void actAddIpfsPath(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();

        jaxrsClient.addIpfsContent(rawAddr, relPath, (URL) null);

        redirectFileList(exchange, rawAddr);
    }

    private void actAddIpfsURL(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();
        URL furl = new URL(qparams.get("url").getFirst());

        jaxrsClient.addIpfsContent(rawAddr, relPath, furl);

        redirectFileList(exchange, rawAddr);
    }

    private void actIpfsGet(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String relPath = qparams.get("path").getFirst();
        String rawAddr = qparams.get("addr").getFirst();
        String cid = qparams.get("cid").getFirst();

        jaxrsClient.getIpfsContent(rawAddr, cid, relPath, null);

        redirectFileList(exchange, rawAddr);
    }

    private void actIpfsSend(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawFromAddr = qparams.get("fromaddr").getFirst();
        String rawToAddr = qparams.get("toaddr").getFirst();
        String cid = qparams.get("cid").getFirst();

        jaxrsClient.sendIpfsContent(rawFromAddr, cid, rawToAddr, null);

        redirectFileList(exchange, rawFromAddr);
    }

    private void actUnregisterIpfs(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        Deque<String> deque = qparams.get("cids");
        String[] cids = deque.toArray(new String[deque.size()]);

        jaxrsClient.unregisterIpfsContent(rawAddr, Arrays.asList(cids));

        redirectFileList(exchange, rawAddr);
    }

    private ByteBuffer actShowLocal(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();

        try (InputStream ins = jaxrsClient.getLocalContent(rawAddr, relPath)) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamUtils.copyStream(ins, baos);

            return ByteBuffer.wrap(baos.toByteArray());
        }
    }

    private void actRemoveLocal(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();

        jaxrsClient.removeLocalContent(rawAddr, relPath);

        redirectFileList(exchange, rawAddr);
    }

    private String pageError(VelocityContext context, Throwable th) {

        String errmsg = th.getMessage();
        if (errmsg == null || errmsg.length() == 0) 
            errmsg = th.toString();
        
        if (th instanceof BitcoinRPCException) {
            errmsg = ((BitcoinRPCException) th).getRPCError().getMessage();
            errmsg = "Blockchain not available: " + errmsg;
        }

        context.put("errmsg", errmsg);

        return "templates/portal-error.vm";
    }

    private String pageIpfsAdd(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String addr = qparams.get("addr").getFirst();

        SAHandle ahandle = findAddressInfo(addr);
        context.put("addr", ahandle);

        return "templates/portal-add.vm";
    }

    private String pageIpfsGet(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String addr = qparams.get("addr").getFirst();
        String path = qparams.get("path").getFirst();
        String cid = qparams.get("cid").getFirst();

        SAHandle ahandle = findAddressInfo(addr);
        SFHandle fhandle = new SFHandle(addr, cid, path, true, true);
        context.put("gatewayUrl", gatewayUrl);
        context.put("addr", ahandle);
        context.put("file", fhandle);

        context.put("treeDataLocal", getTreeData(jaxrsClient.findLocalContent(addr, null), null));

        return "templates/portal-get.vm";
    }

    private String pageIpfsList(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String addr = qparams.get("addr").getFirst();

        SAHandle ahandle = findAddressInfo(addr);
        context.put("addr", ahandle);
        context.put("gatewayUrl", gatewayUrl);

        List<SAHandle> toaddrs = findAddressInfo(null, null).stream()
                .filter(ah -> !ah.getAddress().equals(addr))
                .filter(ah -> ah.getEncKey() != null)
                .collect(Collectors.toList());

        Boolean nosend = toaddrs.isEmpty();

        context.put("treeDataIpfs", getTreeData(jaxrsClient.findIpfsContent(addr, null), nosend));
        context.put("treeDataLocal", getTreeData(jaxrsClient.findLocalContent(addr, null), nosend));

        return "templates/portal-list.vm";
    }

    private String pageQRCode(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String addr = qparams.get("addr").getFirst();

        SAHandle ahandle = findAddressInfo(addr);
        context.put("addr", ahandle);

        return "templates/portal-qr.vm";
    }

    private String pageIpfsSend(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String addr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();
        String cid = qparams.get("cid").getFirst();

        SAHandle ahandle = findAddressInfo(addr);
        List<SAHandle> toaddrs = findAddressInfo(null, null).stream()
                .filter(ah -> !ah.getAddress().equals(addr))
                .filter(ah -> ah.getEncKey() != null)
                .collect(Collectors.toList());

        context.put("gatewayUrl", gatewayUrl);
        context.put("toaddrs", toaddrs);
        context.put("addr", ahandle);
        context.put("file", new SFHandle(addr, cid, relPath, true, true));

        return "templates/portal-send.vm";
    }

    private String pageHome(VelocityContext context) throws Exception {

        List<SAHandle> addrs = findAddressInfo(null, null);

        String envLabel = SystemUtils.getenv(WebUIConfig.ENV_NESSUS_WEBUI_LABEL, "Bob");
        context.put("envLabel", envLabel);
        context.put("addrs", addrs);

        return "templates/portal-home.vm";
    }

    private TreeData getTreeData(List<SFHandle> fhandles, Boolean nosend) {

        TreeData tree = new TreeData();
        Map<Path, TreeNode> nodes = new LinkedHashMap<>();

        for (SFHandle sfh : fhandles) {
            createTreeNode(tree, nodes, sfh, nosend);
        }

        return tree;
    }

    private TreeNode createTreeNode(TreeData tree, Map<Path, TreeNode> nodes, SFHandle sfh, Boolean nosend) {

        Path path = Paths.get(sfh.getPath());
        String cid = sfh.getCid();

        Path ppath = path.getParent();
        TreeNode parent = nodes.get(ppath);

        TreeNode node = new TreeNode(parent, sfh);
        if (nosend != null)
            node.getData().put("nosend", nosend);
        if (cid != null)
            node.getData().put("gatewayUrl", gatewayUrl);
        nodes.put(path, node);

        for (SFHandle child : sfh.getChildren()) {
            createTreeNode(tree, nodes, child, nosend);
        }

        if (parent == null) {
            tree.addNode(node);
        } else {
            parent.addChild(node);
        }

        return node;
    }

    private SAHandle findAddressInfo(String addr) throws IOException {
        AssertArgument.assertNotNull(addr, "Null addr");
        SAHandle ahandle = findAddressInfo(null, addr).stream().findFirst().orElse(null);
        AssertState.assertNotNull(ahandle, "Cannot get address handle for: " + addr);
        return ahandle;
    }

	private List<SAHandle> findAddressInfo(String label, String addr) throws IOException {
		
		List<SAHandle> addrs = jaxrsClient.findAddressInfo(label, addr).stream()
				.filter(ah -> ah.getLabel() != null)
				.filter(ah -> ah.getLabel().length() > 0)
				.collect(Collectors.toList());
		
		return addrs;
	}

    private void redirectHomePage(HttpServerExchange exchange) throws Exception {
        new RedirectHandler("/portal").handleRequest(exchange);
    }

    private void redirectFileList(HttpServerExchange exchange, String rawAddr) throws Exception {
        RedirectHandler handler = new RedirectHandler("/portal/plist?addr=" + rawAddr);
        handler.handleRequest(exchange);
    }

    private ByteBuffer staticContent(HttpServerExchange exchange) throws IOException {
        String path = exchange.getRelativePath();
        return getResource(path);
    }

    private void assertBlockchainAvailable(VelocityContext context) {
        JAXRSClient.assertBlockchainNetworkAvailable(network);
        context.put("blockCount", network.getBlockCount());
    }

    private void assertIpfsAvailable(VelocityContext context) throws IOException {
    	String peerId = ipfsClient.getPeerId();
    	String ipfsHost = gatewayUrl.getHost();
        context.put("ipfsSwarm", String.format("/ip4/%s/tcp/4001/ipfs/%s", ipfsHost, peerId));
        context.put("peerId", peerId);
    }

    private ByteBuffer getResource(String resname) throws IOException {

        InputStream is = getClass().getResourceAsStream(resname);
        if (is == null)
            return null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes = new byte[256];
        int len = is.read(bytes);
        while (len > 0) {
            baos.write(bytes, 0, len);
            len = is.read(bytes);
        }
        return ByteBuffer.wrap(baos.toByteArray());
    }
}
