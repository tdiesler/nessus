package io.nessus.bitcoin.demo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.BlockchainFactory;
import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.bitcoin.BitcoinBlockchain;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.Transaction;

public class ContentProviderMain {
    
    private static final Logger LOG = LoggerFactory.getLogger(ContentProviderMain.class);

    public static void main(String[] args) {

        int port = 8080;
        String host = "localhost";

        Undertow server = Undertow.builder()
            .addHttpListener(port, host, new ContentHandler())
            .build();

        LOG.debug("starting on http://" + host + ":" + port);
        server.start();
    }

    static class ContentHandler implements HttpHandler {

        private final BitcoinBlockchain blockchain;
        private final String address;
        
        ContentHandler() {
            blockchain = (BitcoinBlockchain) BlockchainFactory.getBlockchain(true);
            address = blockchain.getWallet().getRawAddresses(null).get(0);
        }
        
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            
            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/html");
            
            ByteBuffer content = null;
            String path = exchange.getRelativePath();
            if (path.startsWith("/images")) {
                content = imageContent(exchange);
            } else if (path.startsWith("/portal")) {
                content = pageContent(exchange);
            }
            
            if (content != null) {
                exchange.getResponseSender().send(content);
            }
        }

        private ByteBuffer pageContent(HttpServerExchange exchange) throws IOException {
            String path = exchange.getRelativePath();
            
            int step = 1;
            Deque<String> param = exchange.getQueryParameters().get("step");
            step = param != null ? Integer.valueOf(param.getFirst()) : step;
            
            if (step == 3 || step == 4) {
                step = 3;
                Wallet wallet = blockchain.getWallet();
                List<UTXO> utxos = wallet.listUnspent(Arrays.asList(address));
                if (utxos.size() > 0) {
                    for (UTXO utxo : utxos) {
                        String txId = utxo.getTxId();
                        Transaction tx = blockchain.getClient().getTransaction(txId);
                        long received = tx.timeReceived().getTime();
                        long now = System.currentTimeMillis();
                        long secs = (now - received) / 1000;
                        if (secs < 30) {
                            LOG.info(String.format("Allow full access for another %d sec", 30 - secs));
                            step = 4;
                            break;
                        }
                    }
                }
                if (step == 3) {
                    LOG.info(String.format("No UTXO for %s", address));
                }
            }
            
            
            String resource = String.format("%s-%02d.html", path, step);
            
            return getResource(resource);
        }
        
        private ByteBuffer imageContent(HttpServerExchange exchange) throws IOException {
            String path = exchange.getRelativePath();
            return getResource(path);
        }


        private ByteBuffer getResource(String resname) throws IOException {
            
            InputStream is = getClass().getResourceAsStream(resname);
            if (is == null) return null;
            
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
}