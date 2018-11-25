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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
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
import io.nessus.ipfs.jaxrs.JAXRSClient;
import io.nessus.ipfs.jaxrs.SFHandle;
import io.nessus.utils.AssertState;
import io.nessus.utils.StreamUtils;
import io.nessus.utils.SystemUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.util.Headers;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;

public class NessusContentHandler implements HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(NessusContentHandler.class);

    final Blockchain blockchain;
    final Network network;
    final Wallet wallet;

    final JAXRSClient client;
    final VelocityEngine ve;
    final URI gatewayURI;

    // Executor service for async operations
    final ExecutorService executorService;

    // The last executable job
    private Future<Address> lastJob;
    
    private Long blockchainVersion;
    
    NessusContentHandler(JAXRSClient client, Blockchain blockchain, URI gatewayURI) {
        this.blockchain = blockchain;
        this.gatewayURI = gatewayURI;
        this.client = client;

        network = blockchain.getNetwork();
        wallet = blockchain.getWallet();

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
    public void handleRequest(HttpServerExchange exchange) {

        ByteBuffer content = null;
        try {
            
            String path = exchange.getRelativePath();
            if (path.startsWith("/portal")) {
                content = dynamicContent(exchange);
            } else {
                content = staticContent(exchange);
            }
            
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            content = ByteBuffer.wrap(sw.toString().getBytes());
            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/plain");
            LOG.error("Error in: " + exchange.getRequestURI(), ex);
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

        // Assert Blockchain availability
        
        try {
            assertBlockchainAvailable();
        } catch (RuntimeException rte) {
            tmplPath = pageError(context, rte);
        }
        
        if (relPath.startsWith("/portal/addtxt")) {
            actAddText(exchange, context);
        }

        else if (relPath.startsWith("/portal/addurl")) {
            actAddURL(exchange, context);
        }

        else if (relPath.startsWith("/portal/assign")) {
            actAssignLabel(exchange, context);
        }

        else if (relPath.startsWith("/portal/rmlocal")) {
            actRemoveLocalContent(exchange, context);
        }

        else if (relPath.startsWith("/portal/fget")) {
            actFileGet(exchange, context);
        }

        else if (relPath.startsWith("/portal/fshow")) {
            return actFileShow(exchange, context);
        }

        else if (relPath.startsWith("/portal/impkey")) {
            actImportKey(exchange, context);
        }

        else if (relPath.startsWith("/portal/newaddr")) {
            actNewAddress(exchange, context);
        }

        else if (relPath.startsWith("/portal/padd")) {
            tmplPath = pageFileAdd(exchange, context);
        }

        else if (relPath.startsWith("/portal/plist")) {
            tmplPath = pageFileList(exchange, context);
        }

        else if (relPath.startsWith("/portal/pqr")) {
            tmplPath = pageQRCode(exchange, context);
        }

        else if (relPath.startsWith("/portal/psend")) {
            tmplPath = pageSend(exchange, context);
        }

        else if (relPath.startsWith("/portal/regaddr")) {
            actRegisterAddress(exchange, context);
        }

        else if (relPath.startsWith("/portal/sendcid")) {
            actSend(exchange, context);
        }

        else if (relPath.startsWith("/portal/unregaddr")) {
            actUnregisterAddress(exchange, context);
        }

        else if (relPath.startsWith("/portal/rmipfs")) {
            actRemoveIPFSContent(exchange, context);
        }

        else if (tmplPath == null) {
            tmplPath = pageHome(context);
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

    private void actAddText(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();
        String content = qparams.get("content").getFirst();

        client.add(rawAddr, relPath, new ByteArrayInputStream(content.getBytes()));

        redirectFileList(exchange, rawAddr);
    }

    private void actAddURL(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();
        URL furl = new URL(qparams.get("url").getFirst());

        client.add(rawAddr, relPath, furl.openStream());

        redirectFileList(exchange, rawAddr);
    }

    private void actAssignLabel(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String label = qparams.get("label").getFirst();

        Address addr = wallet.findAddress(rawAddr);
        addr.setLabels(Arrays.asList(label));

        redirectHomePage(exchange);
    }

    private void actFileGet(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String relPath = qparams.get("path").getFirst();
        String rawAddr = qparams.get("addr").getFirst();
        String cid = qparams.get("cid").getFirst();

        client.get(rawAddr, cid, relPath, null);

        redirectFileList(exchange, rawAddr);
    }

    private ByteBuffer actFileShow(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();

        try (InputStream ins = client.getLocalContent(rawAddr, relPath)) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamUtils.copyStream(ins, baos);

            return ByteBuffer.wrap(baos.toByteArray());
        }
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

        client.registerAddress(rawAddr);

        redirectHomePage(exchange);
    }

    private void actRemoveIPFSContent(HttpServerExchange exchange, VelocityContext context) throws Exception {
        
        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        Deque<String> deque = qparams.get("cids");
        String[] cids = deque.toArray(new String[deque.size()]);

        client.removeIPFSContent(rawAddr, Arrays.asList(cids));
        
        redirectFileList(exchange, rawAddr);
    }

    private void actRemoveLocalContent(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();

        client.removeLocalContent(rawAddr, relPath);

        redirectFileList(exchange, rawAddr);
    }

    private void actSend(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawFromAddr = qparams.get("fromaddr").getFirst();
        String rawToAddr = qparams.get("toaddr").getFirst();
        String cid = qparams.get("cid").getFirst();

        client.send(rawFromAddr, cid, rawToAddr, null);

        redirectFileList(exchange, rawFromAddr);
    }

    private void actUnregisterAddress(HttpServerExchange exchange, VelocityContext context) throws Exception {
        
        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();

        client.unregisterAddress(rawAddr);
        
        redirectHomePage(exchange);
    }

    private String pageError(VelocityContext context, RuntimeException rte) {
        
        String errmsg = rte.getMessage();
        if (rte instanceof BitcoinRPCException) {
            errmsg = ((BitcoinRPCException) rte).getRPCError().getMessage();
            errmsg = "Blockchain not available: " + errmsg;
        }
        
        context.put("errmsg", errmsg);
        
        return "templates/portal-error.vm";
    }

    private String pageFileAdd(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();

        Address addr = wallet.findAddress(rawAddr);
        AddressDTO paddr = portalAddress(addr, true);
        context.put("addr", paddr);

        return "templates/portal-add.vm";
    }

    private String pageFileList(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();

        Address addr = wallet.findAddress(rawAddr);
        String pubKey = findAddressRegistation(rawAddr);
        AddressDTO paddr = portalAddress(addr, pubKey != null);
        context.put("addr", paddr);

        List<SFHandle> fhandles = new ArrayList<>(client.findIPFSContent(rawAddr, null));
        fhandles.addAll(client.findLocalContent(rawAddr));

        context.put("files", fhandles);
        context.put("gatewayUrl", gatewayURI);

        return "templates/portal-list.vm";
    }

    private String pageQRCode(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();

        Address addr = wallet.findAddress(rawAddr);
        AddressDTO paddr = portalAddress(addr, false);
        context.put("addr", paddr);

        return "templates/portal-qr.vm";
    }

    private String pageSend(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();
        String cid = qparams.get("cid").getFirst();

        Address addr = wallet.findAddress(rawAddr);
        AddressDTO paddr = portalAddress(addr, true);

        List<AddressDTO> toaddrs = new ArrayList<>();
        for (Address aux : getAddressWithLabel(true, true)) {
            if (!addr.equals(aux)) {
                toaddrs.add(portalAddress(aux, true));
            }
        }

        context.put("gatewayUrl", gatewayURI);
        context.put("toaddrs", toaddrs);
        context.put("addr", paddr);
        context.put("file", new SFHandle(cid, rawAddr, relPath, true, true));

        return "templates/portal-send.vm";
    }

    private String pageHome(VelocityContext context) throws Exception {

        List<AddressDTO> addrs = new ArrayList<>();

        for (Address addr : getAddressWithLabel(false, false)) {
            BigDecimal balance = wallet.getBalance(addr);
            String pubKey = findAddressRegistation(addr.getAddress());
            addrs.add(new AddressDTO(addr, balance, pubKey != null));
        }

        String envLabel = SystemUtils.getenv(NessusWebUIConstants.ENV_NESSUS_WEBUI_LABEL, "Bob");

        context.put("envLabel", envLabel);
        context.put("addrs", addrs);

        return "templates/portal-home.vm";
    }

    private List<Address> getAddressWithLabel(boolean requireLabel, boolean requireRegistered) {

        // Get the list of non-change addresses
        List<Address> addrs = wallet.getAddresses().stream()
                .filter(a -> !a.getLabels().contains(Wallet.LABEL_CHANGE))
                .filter(a -> !requireLabel || !a.getLabels().isEmpty())
                .collect(Collectors.toList());

        if (requireRegistered) {
            addrs = addrs.stream()
                .filter(a -> findAddressRegistation(a.getAddress()) != null)
                .collect(Collectors.toList());
        }
        
        return addrs;
    }

    private void redirectHomePage(HttpServerExchange exchange) throws Exception {
        new RedirectHandler("/portal").handleRequest(exchange);
    }

    private void redirectFileList(HttpServerExchange exchange, String rawAddr) throws Exception {
        RedirectHandler handler = new RedirectHandler("/portal/plist?addr=" + rawAddr);
        handler.handleRequest(exchange);
    }

    private AddressDTO portalAddress(Address addr, boolean registered) {
        BigDecimal balance = wallet.getBalance(addr);
        return new AddressDTO(addr, balance, registered);
    }

    private ByteBuffer staticContent(HttpServerExchange exchange) throws IOException {
        String path = exchange.getRelativePath();
        return getResource(path);
    }

    private String findAddressRegistation(String rawAddr) {
        try {
            return client.findAddressRegistation(rawAddr);
        } catch (IOException ex) {
            LOG.error("Error finding address registration", ex);
            return null;
        }
    }

    private void assertBlockchainAvailable() {
        if (blockchainVersion == null) {
            try {
                String networkName = network.getClass().getSimpleName();
                blockchainVersion = network.getNetworkInfo().version();
                LOG.info("{} Version: {}",  networkName, blockchainVersion);
            } catch (BitcoinRPCException rte) {
                String errmsg = rte.getRPCError().getMessage();
                LOG.error("Blockchain not available: {}", errmsg);
                throw rte;
            } catch (RuntimeException rte) {
                LOG.error("Blockchain error", rte);
                throw rte;
            }
        }
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

    public static class AddressDTO {

        public final Address addr;
        public final BigDecimal balance;
        public final boolean registered;

        private AddressDTO(Address addr, BigDecimal balance, boolean registered) {
            this.addr = addr;
            this.registered = registered;
            this.balance = balance;
        }

        public String getLabel() {
            List<String> labels = addr.getLabels();
            return labels.size() > 0 ? labels.get(0) : "";
        }

        public String getAddress() {
            return addr.getAddress();
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public boolean isRegistered() {
            return registered;
        }

        public boolean isWatchOnly() {
            return addr.isWatchOnly();
        }

        @Override
        public String toString() {
            return String.format("[addr=%s, ro=%b, label=%s, reg=%b, bal=%.4f]", getAddress(), isWatchOnly(), getLabel(), isRegistered(), getBalance());
        }
    }
}
