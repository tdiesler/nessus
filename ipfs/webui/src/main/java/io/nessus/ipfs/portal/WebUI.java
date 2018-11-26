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

import java.net.URI;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.core.ipfs.IPFSClient;
import io.nessus.ipfs.jaxrs.JAXRSApplication;
import io.nessus.ipfs.jaxrs.JAXRSClient;
import io.nessus.ipfs.jaxrs.JAXRSConstants;
import io.nessus.ipfs.jaxrs.JAXRSSanityCheck;
import io.nessus.utils.SystemUtils;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

public class WebUI {

    private static final Logger LOG = LoggerFactory.getLogger(WebUI.class);

    public static final String ENV_NESSUS_WEBUI_ADDR = "NESSUS_WEBUI_ADDR";
    public static final String ENV_NESSUS_WEBUI_PORT = "NESSUS_WEBUI_PORT";
    public static final String ENV_NESSUS_WEBUI_LABEL = "NESSUS_WEBUI_LABEL";
    
    public static void main(String[] args) throws Exception {

        JAXRSSanityCheck.verifyPlatform();

        WebUI webUI = new WebUI (args);
        webUI.start();
    }

    public WebUI(String[] args) {
    }

    protected String getApplicationName() {
        return "Nessus";
    }
    
    protected void start() throws Exception {
        
        String envHost = SystemUtils.getenv(IPFSClient.ENV_IPFS_GATEWAY_ADDR, "127.0.0.1");
        String envPort = SystemUtils.getenv(IPFSClient.ENV_IPFS_GATEWAY_PORT, "8080");
        URI gatewayURI = new URI(String.format("http://%s:%s/ipfs", envHost, envPort));
        LOG.info("IPFS Gateway: {}", gatewayURI);

        URL rpcUrl = JAXRSApplication.blockchainURL();
        Class<Blockchain> bcclass = JAXRSApplication.blockchainClass();
        Blockchain blockchain = BlockchainFactory.getBlockchain(rpcUrl, bcclass);
        JAXRSApplication.logNetworkVersionFailsafe(blockchain.getNetwork());
        
        envHost = SystemUtils.getenv(JAXRSConstants.ENV_NESSUS_JAXRS_ADDR, "127.0.0.1");
        envPort = SystemUtils.getenv(JAXRSConstants.ENV_NESSUS_JAXRS_PORT, "8081");
        URI jaxrsURI = new URI(String.format("http://%s:%s/nessus", envHost, envPort));
        LOG.info("Nessus JAXRS: {}", jaxrsURI);

        JAXRSClient jaxrsClient = new JAXRSClient(jaxrsURI);

        envHost = SystemUtils.getenv(ENV_NESSUS_WEBUI_ADDR, "0.0.0.0");
        envPort = SystemUtils.getenv(ENV_NESSUS_WEBUI_PORT, "8082");
        LOG.info("{} WebUI: http://" + envHost + ":" + envPort + "/portal", getApplicationName());

        HttpHandler contentHandler = createHttpHandler(gatewayURI, blockchain, jaxrsClient);
        Undertow server = Undertow.builder()
                .addHttpListener(Integer.valueOf(envPort), envHost, contentHandler)
                .build();

        server.start();
    }

    protected HttpHandler createHttpHandler(URI gatewayURI, Blockchain blockchain, JAXRSClient jaxrsClient) {
        return new ContentHandler(jaxrsClient, blockchain, gatewayURI);
    }
}
