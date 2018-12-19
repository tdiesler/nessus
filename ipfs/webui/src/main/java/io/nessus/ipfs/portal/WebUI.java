package io.nessus.ipfs.portal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.ipfs.ContentManagerConfig;
import io.nessus.ipfs.jaxrs.JAXRSClient;
import io.nessus.ipfs.jaxrs.JAXRSSanityCheck;
import io.nessus.utils.SystemUtils;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

public class WebUI {

    private static final Logger LOG = LoggerFactory.getLogger(WebUI.class);

    static final String implVersion;
    static final String implBuild;
    static {
        try (InputStream ins = ContentHandler.class.getResourceAsStream("/" + JarFile.MANIFEST_NAME)) {
            Manifest manifest = new Manifest(ins);
            Attributes attribs = manifest.getMainAttributes();
            implVersion = attribs.getValue("Implementation-Version");
            implBuild = attribs.getValue("Implementation-Build");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    final String webuiHost;
    final String webuiPort;
    final URL gatewayUrl;
    
    final Blockchain blockchain;
    final JAXRSClient jaxrsClient;
    
    public static void main(String[] args) throws Exception {

    	WebUIConfig config = new WebUIConfig();
        CmdLineParser parser = new CmdLineParser(config);
        
        try {
            parser.parseArgument(args);
        } catch (CmdLineException ex) {
        	helpScreen(parser);
            throw ex;
        }

        if (config.help) {
        	helpScreen(parser);
        } else {
            JAXRSSanityCheck.verifyPlatform();
            WebUI webUI = new WebUI (config);
            webUI.start();
        }
    }

    public WebUI(WebUIConfig config) throws Exception {
        
        LOG.info("{} Version: {} Build: {}", getApplicationName(), implVersion, implBuild);
        
        blockchain = config.getBlockchain();
        JAXRSClient.logBlogchainNetworkAvailable(blockchain.getNetwork());
        
        String envHost = SystemUtils.getenv(ContentManagerConfig.ENV_IPFS_GATEWAY_ADDR, "127.0.0.1");
        String envPort = SystemUtils.getenv(ContentManagerConfig.ENV_IPFS_GATEWAY_PORT, "8080");
        gatewayUrl = new URL(String.format("http://%s:%s/ipfs", envHost, envPort));
        LOG.info("IPFS Gateway: {}", gatewayUrl);

        URL jaxrsUrl = config.getJaxrsUrl();
        LOG.info("Nessus JAXRS: {}", jaxrsUrl);

        jaxrsClient = new JAXRSClient(jaxrsUrl);

        webuiHost = SystemUtils.getenv(WebUIConfig.ENV_NESSUS_WEBUI_ADDR, "0.0.0.0");
        webuiPort = SystemUtils.getenv(WebUIConfig.ENV_NESSUS_WEBUI_PORT, "8082");
        LOG.info("{} WebUI: http://" + envHost + ":" + envPort + "/portal", getApplicationName());
    }

    protected String getApplicationName() {
        return "Nessus";
    }
    
    protected void start() throws Exception {
        

        HttpHandler contentHandler = createHttpHandler(blockchain, jaxrsClient, gatewayUrl);
        Undertow server = Undertow.builder()
                .addHttpListener(Integer.valueOf(webuiPort), webuiHost, contentHandler)
                .build();

        server.start();
    }

    protected HttpHandler createHttpHandler(Blockchain blockchain, JAXRSClient jaxrsClient, URL gatewayUrl) throws Exception {
        return new ContentHandler(jaxrsClient, blockchain, gatewayUrl.toURI());
    }

    private static void helpScreen(CmdLineParser parser) {
        System.err.println("run-webui [options...]");
        parser.printUsage(System.err);
    }
}
