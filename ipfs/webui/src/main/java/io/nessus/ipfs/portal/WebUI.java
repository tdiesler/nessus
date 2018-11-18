package io.nessus.ipfs.portal;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.core.ipfs.IPFSClient;
import io.nessus.ipfs.jaxrs.JAXRSApplication;
import io.nessus.ipfs.jaxrs.JAXRSClient;
import io.nessus.ipfs.jaxrs.JAXRSConstants;
import io.nessus.ipfs.jaxrs.JAXRSSanityCheck;
import io.nessus.utils.SystemUtils;
import io.undertow.Undertow;

public class WebUI {

    private static final Logger LOG = LoggerFactory.getLogger(WebUI.class);

    public static void main(String[] args) throws Exception {

        JAXRSSanityCheck.verifyPlatform();

        try {

            String envHost = SystemUtils.getenv(IPFSClient.ENV_IPFS_GATEWAY_ADDR, "127.0.0.1");
            String envPort = SystemUtils.getenv(IPFSClient.ENV_IPFS_GATEWAY_PORT, "8080");
            URI gatewayURI = new URI(String.format("http://%s:%s/ipfs", envHost, envPort));
            LOG.info("IPFS Gateway: {}", gatewayURI);

            URL rpcUrl = JAXRSApplication.blockchainURL();
            Class<Blockchain> bcclass = JAXRSApplication.blockchainClass();
            Blockchain blockchain = BlockchainFactory.getBlockchain(rpcUrl, bcclass);
            Network network = blockchain.getNetwork();
            String networkName = network.getClass().getSimpleName();
            LOG.info("{} Version: {}",  networkName, network.getNetworkInfo().version());

            envHost = SystemUtils.getenv(JAXRSConstants.ENV_NESSUS_JAXRS_ADDR, "127.0.0.1");
            envPort = SystemUtils.getenv(JAXRSConstants.ENV_NESSUS_JAXRS_PORT, "8081");
            URI jaxrsURI = new URI(String.format("http://%s:%s/nessus", envHost, envPort));
            LOG.info("Nessus JAXRS: {}", jaxrsURI);

            JAXRSClient jaxrsClient = new JAXRSClient(jaxrsURI);

            envHost = SystemUtils.getenv(WebUIConstants.ENV_NESSUS_WEBUI_ADDR, "0.0.0.0");
            envPort = SystemUtils.getenv(WebUIConstants.ENV_NESSUS_WEBUI_PORT, "8082");
            LOG.info("Nessus WebUI: http://" + envHost + ":" + envPort + "/portal");

            Undertow server = Undertow.builder()
                    .addHttpListener(Integer.valueOf(envPort), envHost, new ContentHandler(jaxrsClient, blockchain, gatewayURI))
                    .build();

            server.start();

        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
