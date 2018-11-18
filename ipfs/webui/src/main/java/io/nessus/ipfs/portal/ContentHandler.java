package io.nessus.ipfs.portal;

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
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
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
import io.nessus.utils.StreamUtils;
import io.nessus.utils.SystemUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.util.Headers;

public class ContentHandler implements HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ContentHandler.class);

    final Blockchain blockchain;
    final Network network;
    final Wallet wallet;

    final JAXRSClient client;
    final VelocityEngine ve;
    final URI gatewayURI;

    ContentHandler(JAXRSClient client, Blockchain blockchain, URI gatewayURI) {
        this.blockchain = blockchain;
        this.gatewayURI = gatewayURI;
        this.client = client;

        network = blockchain.getNetwork();
        wallet = blockchain.getWallet();

        ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {

        ByteBuffer content = null;
        try {
            String path = exchange.getRelativePath();
            if (path.startsWith("/portal")) {
                content = pageContent(exchange);
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

    private ByteBuffer pageContent(HttpServerExchange exchange) throws Exception {

        String relPath = exchange.getRelativePath();

        String tmplPath = null;
        VelocityContext context = new VelocityContext();

        // Action add text

        if (relPath.startsWith("/portal/addtxt")) {

            actAddText(exchange, context);
        }

        // Action add URL

        else if (relPath.startsWith("/portal/addurl")) {

            actAddURL(exchange, context);
        }

        // Action assign Label

        else if (relPath.startsWith("/portal/assign")) {

            actAssignLabel(exchange, context);
        }

        // Action file delete

        else if (relPath.startsWith("/portal/fdel")) {

            actFileDel(exchange, context);
        }

        // Action file get

        else if (relPath.startsWith("/portal/fget")) {

            actFileGet(exchange, context);
        }

        // Action file show

        else if (relPath.startsWith("/portal/fshow")) {

            return actFileShow(exchange, context);
        }

        // Action new address

        else if (relPath.startsWith("/portal/newaddr")) {

            actNewAddress(exchange, context);
        }

        // Action import privkey

        else if (relPath.startsWith("/portal/impkey")) {

            actImportKey(exchange, context);
        }

        // Action register address

        else if (relPath.startsWith("/portal/regaddr")) {

            actRegisterAddress(exchange, context);
        }

        // Action send IPFS file

        else if (relPath.startsWith("/portal/sendcid")) {

            actSend(exchange, context);
        }

        // Page file add

        else if (relPath.startsWith("/portal/padd")) {

            tmplPath = pageFileAdd(exchange, context);
        }

        // Page file list

        else if (relPath.startsWith("/portal/plist")) {

            tmplPath = pageFileList(exchange, context);
        }

        // Page file send

        else if (relPath.startsWith("/portal/psend")) {

            tmplPath = pageSend(exchange, context);
        }

        // Home page

        else {

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

    private void actAssignLabel(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String label = qparams.get("label").getFirst();

        Address addr = wallet.findAddress(rawAddr);
        addr.setLabels(Arrays.asList(label));

        redirectHomePage(exchange);
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

    private void actRegisterAddress(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();

        client.registerAddress(rawAddr);

        redirectHomePage(exchange);
    }

    private void actNewAddress(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String label = qparams.get("label").getFirst();

        wallet.newAddress(label);

        redirectHomePage(exchange);
    }

    private void actFileDel(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();

        client.deleteLocalContent(rawAddr, relPath);

        redirectFileList(exchange, rawAddr);
    }

    private void actFileGet(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String relPath = qparams.get("path").getFirst();
        String rawAddr = qparams.get("addr").getFirst();
        String cid = qparams.get("cid").getFirst();

        client.get(rawAddr, cid, relPath, 10000L);

        redirectFileList(exchange, rawAddr);
    }

    private void actImportKey(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String key = qparams.get("impkey").getFirst();
        String label = qparams.get("label").getFirst();

        if (wallet.isP2PKH(key)) {
            LOG.info("Importing watch only address: {}", key);
            wallet.importAddress(key, Arrays.asList(label), false);
        } else {
            // Import key asynch with rescan
            new Thread(new Runnable() {
                public void run() {
                    LOG.info("Importing private key: P**************");
                    wallet.importPrivateKey(key, Arrays.asList(label), true);
                }
            }).start();
        }

        redirectHomePage(exchange);
    }

    private void actSend(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawFromAddr = qparams.get("fromaddr").getFirst();
        String rawToAddr = qparams.get("toaddr").getFirst();
        String cid = qparams.get("cid").getFirst();

        client.send(rawFromAddr, cid, rawToAddr, 10000L);

        redirectFileList(exchange, rawFromAddr);
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
        String pubKey = client.findAddressRegistation(rawAddr);
        AddressDTO paddr = portalAddress(addr, pubKey != null);
        context.put("addr", paddr);

        List<SFHandle> fhandles = new ArrayList<>(client.findIPFSContent(rawAddr, 10000L));
        fhandles.addAll(client.findLocalContent(rawAddr));

        context.put("files", fhandles);
        context.put("gatewayUrl", gatewayURI);

        return "templates/portal-list.vm";
    }

    private String pageSend(HttpServerExchange exchange, VelocityContext context) throws Exception {

        Map<String, Deque<String>> qparams = exchange.getQueryParameters();
        String rawAddr = qparams.get("addr").getFirst();
        String relPath = qparams.get("path").getFirst();
        String cid = qparams.get("cid").getFirst();

        Address addr = wallet.findAddress(rawAddr);
        AddressDTO paddr = portalAddress(addr, true);

        List<AddressDTO> toaddrs = new ArrayList<>();
        for (Address aux : getAddressWithLabel()) {
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

        for (Address addr : getAddressWithLabel()) {
            BigDecimal balance = wallet.getBalance(addr);
            String pubKey = client.findAddressRegistation(addr.getAddress());
            addrs.add(new AddressDTO(addr, balance, pubKey != null));
        }

        String envLabel = SystemUtils.getenv(WebUIConstants.ENV_NESSUS_WEBUI_LABEL, "Bob");

        context.put("envLabel", envLabel);
        context.put("addrs", addrs);

        return "templates/portal-home.vm";
    }

    // [TODO #12] Wallet generates unwanted addresses for default account
    // Here we filter addresses for the default acount, if we already have labeled addresses
    private List<Address> getAddressWithLabel() {

        // Get the list of non-change addresses
        List<Address> addrs = wallet.getAddresses().stream()
                .filter(a -> !a.getLabels().contains(Wallet.LABEL_CHANGE))
                .collect(Collectors.toList());

        // Remove addrs that have no label
        List<Address> filtered = addrs.stream()
                .filter(a -> !a.getLabels().contains(""))
                .collect(Collectors.toList());

        return filtered.isEmpty() ? addrs : filtered;
    }

    private void redirectHomePage(HttpServerExchange exchange) throws Exception {
        new RedirectHandler("/portal").handleRequest(exchange);
    }

    private void redirectFileList(HttpServerExchange exchange, String rawAddr) throws Exception {
        RedirectHandler handler = new RedirectHandler("/portal/plist?addr=" + rawAddr);
        handler.handleRequest(exchange);
    }

    private AddressDTO portalAddress(Address addr, boolean registered) throws GeneralSecurityException {
        BigDecimal balance = wallet.getBalance(addr);
        return new AddressDTO(addr, balance, registered);
    }

    private ByteBuffer staticContent(HttpServerExchange exchange) throws IOException {
        String path = exchange.getRelativePath();
        return getResource(path);
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
