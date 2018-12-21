package io.nessus.ipfs.portal;

import java.net.MalformedURLException;
import java.net.URL;

import org.kohsuke.args4j.Option;

import io.nessus.ipfs.core.AbstractConfig;
import io.nessus.ipfs.jaxrs.JAXRSConfig;
import io.nessus.utils.SystemUtils;

public final class WebUIConfig extends AbstractConfig {

    public static final String ENV_NESSUS_WEBUI_ADDR = "NESSUS_WEBUI_ADDR";
    public static final String ENV_NESSUS_WEBUI_PORT = "NESSUS_WEBUI_PORT";
    public static final String ENV_NESSUS_WEBUI_CONTEXT_PATH = "NESSUS_WEBUI_CONTEXT_PATH";
    public static final String ENV_NESSUS_WEBUI_LABEL = "NESSUS_WEBUI_LABEL";
    
    public static final String DEFAULT_WEBUI_HOST = "0.0.0.0";
    public static final int DEFAULT_WEBUI_PORT = 8082;
    public static final String DEFAULT_WEBUI_CONTEXT_PATH = "portal";
    
    @Option(name = "--jaxrs-host", usage = "The Nessus JAXRS host")
    String jaxrsHost = JAXRSConfig.DEFAULT_JAXRS_HOST;

    @Option(name = "--jaxrs-port", usage = "The Nessus JAXRS port")
    int jaxrsPort = JAXRSConfig.DEFAULT_JAXRS_PORT;

    @Option(name = "--jaxrs-path", usage = "The JAXRS context path")
    String jaxrsPath = JAXRSConfig.DEFAULT_JAXRS_CONTEXT_PATH;

    @Option(name = "--webui-host", usage = "The WebUI host")
    String webuiHost = DEFAULT_WEBUI_HOST;

    @Option(name = "--webui-port", usage = "The WebUI port")
    int webuiPort = DEFAULT_WEBUI_PORT;

    @Option(name = "--webui-path", usage = "The WebUI context path")
    String webuiPath = DEFAULT_WEBUI_CONTEXT_PATH;

    public WebUIConfig() {
    }
    
    private WebUIConfig(String ipfsAddr, String bcImpl, String bcUrl, String bcHost, int bcPort, String bcUser, 
    		String bcPass, String jaxrsHost, int jaxrsPort, String jaxrsPath, String webuiHost, int webuiPort, String webuiPath) {
    	super(ipfsAddr, bcImpl, bcUrl, bcHost, bcPort, bcUser, bcPass);
        this.jaxrsHost = jaxrsHost;
        this.jaxrsPort = jaxrsPort;
    	this.jaxrsPath = jaxrsPath;
    	this.webuiHost = webuiHost;
    	this.webuiPort = webuiPort;
    	this.webuiPath = webuiPath;
    }

    public URL getJaxrsUrl() throws MalformedURLException {
        
        if (JAXRSConfig.DEFAULT_JAXRS_HOST.equals(jaxrsHost))
            jaxrsHost = SystemUtils.getenv(JAXRSConfig.ENV_NESSUS_JAXRS_ADDR, jaxrsHost);
        
        if (JAXRSConfig.DEFAULT_JAXRS_PORT == jaxrsPort)
            jaxrsPort = Integer.parseInt(SystemUtils.getenv(JAXRSConfig.ENV_NESSUS_JAXRS_PORT, "" + jaxrsPort));
        
        if (JAXRSConfig.DEFAULT_JAXRS_CONTEXT_PATH == jaxrsPath) 
        	jaxrsPath = SystemUtils.getenv(JAXRSConfig.ENV_NESSUS_JAXRS_CONTEXT_PATH, "" + jaxrsPath);
        
        return new URL(String.format("http://%s:%s/%s", jaxrsHost, jaxrsPort, jaxrsPath));   
    }
    
    public URL getWebUiUrl() throws MalformedURLException {
        
        if (DEFAULT_WEBUI_HOST.equals(webuiHost))
        	webuiHost = SystemUtils.getenv(ENV_NESSUS_WEBUI_ADDR, webuiHost);
        
        if (DEFAULT_WEBUI_PORT == webuiPort)
            webuiPort = Integer.parseInt(SystemUtils.getenv(ENV_NESSUS_WEBUI_PORT, "" + webuiPort));
        
        if (DEFAULT_WEBUI_CONTEXT_PATH == webuiPath)
        	webuiPath = SystemUtils.getenv(ENV_NESSUS_WEBUI_CONTEXT_PATH, "" + webuiPath);
        
        return new URL(String.format("http://%s:%s/%s", webuiHost, webuiPort, webuiPath));   
    }
    
    public static class WebUIConfigBuilder extends AbstractConfigBuilder<WebUIConfigBuilder, WebUIConfig> {
        
        String jaxrsPath = JAXRSConfig.DEFAULT_JAXRS_CONTEXT_PATH;
        String jaxrsHost = JAXRSConfig.DEFAULT_JAXRS_HOST;
        int jaxrsPort = JAXRSConfig.DEFAULT_JAXRS_PORT;
        String webuiHost = DEFAULT_WEBUI_HOST;
        int webuiPort = DEFAULT_WEBUI_PORT;
        String webuiPath = DEFAULT_WEBUI_CONTEXT_PATH;
        
        public WebUIConfigBuilder jaxrsHost(String jaxrsHost) {
            this.jaxrsHost = jaxrsHost;
            return this;
        }
        
        public WebUIConfigBuilder jaxrsPort(int jaxrsPort) {
            this.jaxrsPort = jaxrsPort;
            return this;
        }
        
        public WebUIConfigBuilder jaxrsPath(String jaxrsPath) {
            this.jaxrsPath = jaxrsPath;
            return this;
        }
        
        public WebUIConfigBuilder webuiHost(String webuiHost) {
            this.webuiHost = webuiHost;
            return this;
        }
        
        public WebUIConfigBuilder webuiPort(int webuiPort) {
            this.webuiPort = webuiPort;
            return this;
        }
        
        public WebUIConfigBuilder webuiPath(String webuiPath) {
            this.webuiPath = webuiPath;
            return this;
        }
        
        public WebUIConfig build() {
            return new WebUIConfig(ipfsAddr, bcImpl, bcUrl, bcHost, bcPort, bcUser, bcPass, jaxrsHost, jaxrsPort, jaxrsPath, webuiHost, webuiPort, webuiPath);
        }
    }
}