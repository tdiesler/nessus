package io.nessus.ipfs.portal;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.ipfs.jaxrs.JAXRSSanityCheck;
import io.nessus.utils.SystemUtils;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

public class WebUI {

    private static final Logger LOG = LoggerFactory.getLogger(WebUI.class);

    private static final String implVersion;
    private static final String implBuild;
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
    
    final WebUIConfig config;
    final String webuiHost;
    final String webuiPort;
    
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
    	this.config = config;
        
        LOG.info("{} Version: {} Build: {}", getApplicationName(), implVersion, implBuild);
        
        webuiHost = SystemUtils.getenv(WebUIConfig.ENV_NESSUS_WEBUI_ADDR, "0.0.0.0");
        webuiPort = SystemUtils.getenv(WebUIConfig.ENV_NESSUS_WEBUI_PORT, "8082");
        LOG.info("{} WebUI: http://" + webuiHost + ":" + webuiPort + "/portal", getApplicationName());
    }

    static String getImplVersion() {
		return implVersion;
	}

	static String getImplBuild() {
		boolean snapshot = implVersion != null && implVersion.endsWith("SNAPSHOT");
		return snapshot ? implBuild : "";
	}

	protected String getApplicationName() {
        return "Nessus";
    }
    
    protected void start() throws Exception {
        
        HttpHandler contentHandler = createHttpHandler(config);
        Undertow server = Undertow.builder()
                .addHttpListener(Integer.valueOf(webuiPort), webuiHost, contentHandler)
                .build();

        server.start();
    }

    protected HttpHandler createHttpHandler(WebUIConfig config) throws Exception {
        return new ContentHandler(config);
    }

    private static void helpScreen(CmdLineParser parser) {
        System.err.println("run-webui [options...]");
        parser.printUsage(System.err);
    }
}
