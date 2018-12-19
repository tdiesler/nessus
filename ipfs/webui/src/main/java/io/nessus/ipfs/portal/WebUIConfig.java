package io.nessus.ipfs.portal;

import java.net.MalformedURLException;
import java.net.URL;

import org.kohsuke.args4j.Option;

import io.nessus.ipfs.BlockchainConfig;
import io.nessus.ipfs.jaxrs.JAXRSConfig;
import io.nessus.ipfs.jaxrs.JAXRSConstants;
import io.nessus.utils.SystemUtils;

public final class WebUIConfig extends BlockchainConfig {

    public static final String ENV_NESSUS_WEBUI_ADDR = "NESSUS_WEBUI_ADDR";
    public static final String ENV_NESSUS_WEBUI_PORT = "NESSUS_WEBUI_PORT";
    public static final String ENV_NESSUS_WEBUI_LABEL = "NESSUS_WEBUI_LABEL";
    
    @Option(name = "--jaxrs-host", usage = "The Nessus JAXRS host")
    String jaxrsHost = JAXRSConfig.DEFAULT_JAXRS_HOST;

    @Option(name = "--jaxrs-port", usage = "The Nessus JAXRS port")
    int jaxrsPort = JAXRSConfig.DEFAULT_JAXRS_PORT;

    @Option(name = "--jaxrs-path", usage = "The JAXRS context path")
    String jaxrsPath = JAXRSConfig.DEFAULT_JAXRS_CONTEXT_PATH;

    public WebUIConfig() {
    }
    
    private WebUIConfig(String bcImpl, String bcUrl, String bcHost, int bcPort, String bcUser, String bcPass, 
    		String ipfsAddr, String jaxrsHost, int jaxrsPort, String jaxrsPath) {
    	super(bcImpl, bcUrl, bcHost, bcPort, bcUser, bcPass);
        this.jaxrsHost = jaxrsHost;
        this.jaxrsPort = jaxrsPort;
    	this.jaxrsPath = jaxrsPath;
    }

    public URL getJaxrsUrl() throws MalformedURLException {
        
        if (JAXRSConfig.DEFAULT_JAXRS_HOST.equals(jaxrsHost))
            jaxrsHost = SystemUtils.getenv(JAXRSConstants.ENV_NESSUS_JAXRS_ADDR, jaxrsHost);
        
        if (JAXRSConfig.DEFAULT_JAXRS_PORT == jaxrsPort)
            jaxrsPort = Integer.parseInt(SystemUtils.getenv(JAXRSConstants.ENV_NESSUS_JAXRS_PORT, "" + jaxrsPort));
        
        return new URL(String.format("http://%s:%s/nessus", jaxrsHost, jaxrsPort));   
    }
    
    public static class WebUIConfigBuilder extends AbstractConfigBuilder<WebUIConfigBuilder, WebUIConfig> {
        
        String jaxrsPath = JAXRSConfig.DEFAULT_JAXRS_CONTEXT_PATH;
        String jaxrsHost = JAXRSConfig.DEFAULT_JAXRS_HOST;
        int jaxrsPort = JAXRSConfig.DEFAULT_JAXRS_PORT;
        String ipfsAddr = JAXRSConfig.DEFAULT_IPFS_ADDR;
        
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
        
        public WebUIConfig build() {
            return new WebUIConfig(bcImpl, bcUrl, bcHost, bcPort, bcUser, bcPass, ipfsAddr, jaxrsHost, jaxrsPort, jaxrsPath);
        }
    }
}