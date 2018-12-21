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
        
        String buildNumber = getImplBuild() != null ? "Build: " + getImplBuild() : "";
        LOG.info("{} Version: {} {}", getApplicationName(), getImplVersion(), buildNumber);
    }

    static String getImplVersion() {
		return implVersion;
	}

	static String getImplBuild() {
		boolean snapshot = implVersion != null && implVersion.endsWith("SNAPSHOT");
		return snapshot ? implBuild : null;
	}

	protected String getApplicationName() {
        return "Nessus";
    }
    
    protected void start() throws Exception {
        
        HttpHandler contentHandler = createHttpHandler(getApplicationName(), config);
        Undertow server = Undertow.builder()
                .addHttpListener(config.webuiPort, config.webuiHost, contentHandler)
                .build();

        server.start();
    }

    protected HttpHandler createHttpHandler(String appName, WebUIConfig config) throws Exception {
        return new ContentHandler(appName, config);
    }

    private static void helpScreen(CmdLineParser parser) {
        System.err.println("run-webui [options...]");
        parser.printUsage(System.err);
    }
}
