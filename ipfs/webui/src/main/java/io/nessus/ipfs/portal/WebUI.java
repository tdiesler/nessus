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
import io.nessus.BlockchainFactory;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.jaxrs.JaxrsClient;
import io.nessus.ipfs.jaxrs.JaxrsConfig;
import io.nessus.ipfs.jaxrs.JaxrsSanityCheck;
import io.nessus.utils.SystemUtils;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

public class WebUI {

    private static final Logger LOG = LoggerFactory.getLogger(WebUI.class);

    public static final String ENV_NESSUS_WEBUI_ADDR = "NESSUS_WEBUI_ADDR";
    public static final String ENV_NESSUS_WEBUI_PORT = "NESSUS_WEBUI_PORT";
    public static final String ENV_NESSUS_WEBUI_LABEL = "NESSUS_WEBUI_LABEL";
    
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
    
    public static void main(String[] args) throws Exception {

        JaxrsConfig config = new JaxrsConfig();
        CmdLineParser parser = new CmdLineParser(config);
        
        try {
            parser.parseArgument(args);
        } catch (CmdLineException ex) {
            parser.printUsage(System.err);
            throw ex;
        }

        JaxrsSanityCheck.verifyPlatform();

        WebUI webUI = new WebUI (config);
        webUI.start();
    }

    final String webuiHost;
    final String webuiPort;
    final URL gatewayUrl;
    
    final Blockchain blockchain;
    final JaxrsClient jaxrsClient;
    
    public WebUI(JaxrsConfig config) throws Exception {
        
        LOG.info("{} Version: {} Build: {}", getApplicationName(), implVersion, implBuild);
        
        URL bcUrl = config.getBlockchainUrl();
        Class<Blockchain> bcImpl = config.getBlockchainImpl();
        blockchain = BlockchainFactory.getBlockchain(bcUrl, bcImpl);
        JaxrsClient.logBlogchainNetworkAvailable(blockchain.getNetwork());
        
        String envHost = SystemUtils.getenv(IPFSClient.ENV_IPFS_GATEWAY_ADDR, "127.0.0.1");
        String envPort = SystemUtils.getenv(IPFSClient.ENV_IPFS_GATEWAY_PORT, "8080");
        gatewayUrl = new URL(String.format("http://%s:%s/ipfs", envHost, envPort));
        LOG.info("IPFS Gateway: {}", gatewayUrl);

        URL jaxrsUrl = config.getJaxrsUrl();
        LOG.info("Nessus JAXRS: {}", jaxrsUrl);

        jaxrsClient = new JaxrsClient(jaxrsUrl);

        webuiHost = SystemUtils.getenv(ENV_NESSUS_WEBUI_ADDR, "0.0.0.0");
        webuiPort = SystemUtils.getenv(ENV_NESSUS_WEBUI_PORT, "8082");
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

    protected HttpHandler createHttpHandler(Blockchain blockchain, JaxrsClient jaxrsClient, URL gatewayUrl) throws Exception {
        return new ContentHandler(jaxrsClient, blockchain, gatewayUrl.toURI());
    }
}
