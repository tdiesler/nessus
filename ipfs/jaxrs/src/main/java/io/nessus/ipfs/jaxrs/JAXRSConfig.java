package io.nessus.ipfs.jaxrs;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import org.kohsuke.args4j.Option;

import io.nessus.ipfs.ContentManagerConfig;
import io.nessus.utils.SystemUtils;

public final class JAXRSConfig extends ContentManagerConfig {

    public static final String ENV_NESSUS_JAXRS_ADDR = "NESSUS_JAXRS_ADDR";
    public static final String ENV_NESSUS_JAXRS_PORT = "NESSUS_JAXRS_PORT";
    public static final String ENV_NESSUS_JAXRS_CONTEXT_PATH = "NESSUS_JAXRS_CONTEXT_PATH";
    
    public static final String DEFAULT_JAXRS_CONTEXT_PATH = "nessus";
    public static final String DEFAULT_JAXRS_HOST = "0.0.0.0";
	public static final int DEFAULT_JAXRS_PORT = 8081;

    @Option(name = "--jaxrs-host", usage = "The Nessus JAXRS host")
    String jaxrsHost = DEFAULT_JAXRS_HOST;

    @Option(name = "--jaxrs-port", usage = "The Nessus JAXRS port")
    int jaxrsPort = DEFAULT_JAXRS_PORT;

    @Option(name = "--jaxrs-path", usage = "The JAXRS context path")
    String jaxrsPath = DEFAULT_JAXRS_CONTEXT_PATH;

    public JAXRSConfig() {
    }
    
    private JAXRSConfig(String ipfsAddr, String bcImpl, String bcUrl, String bcHost, int bcPort, String bcUser, 
    		String bcPass, long ipfsTimeout, int ipfsAttempts, int ipfsThreads, Path dataDir, boolean overwrite, String jaxrsHost, int jaxrsPort, String jaxrsPath) {
    	super(bcImpl, bcUrl, bcHost, bcPort, bcUser, bcPass, ipfsAddr, ipfsTimeout, ipfsAttempts, ipfsThreads, dataDir, overwrite);
        this.jaxrsHost = jaxrsHost;
        this.jaxrsPort = jaxrsPort;
    	this.jaxrsPath = jaxrsPath;
    }

    public URL getJaxrsUrl() throws MalformedURLException {
        
        if (DEFAULT_JAXRS_HOST.equals(jaxrsHost))
            jaxrsHost = SystemUtils.getenv(ENV_NESSUS_JAXRS_ADDR, jaxrsHost);
        
        if (DEFAULT_JAXRS_PORT == jaxrsPort)
            jaxrsPort = Integer.parseInt(SystemUtils.getenv(ENV_NESSUS_JAXRS_PORT, "" + jaxrsPort));
        
        if (DEFAULT_JAXRS_CONTEXT_PATH == jaxrsPath) 
        	jaxrsPath = SystemUtils.getenv(ENV_NESSUS_JAXRS_CONTEXT_PATH, "" + jaxrsPath);
        
        return new URL(String.format("http://%s:%s/%s", jaxrsHost, jaxrsPort, jaxrsPath));   
    }
    
    public String toString() {
        return String.format("[dataDir=%s, timeout=%s, attempts=%s, threads=%s, overwrite=%b]", 
                dataDir, ipfsTimeout, ipfsAttempts, ipfsThreads, overwrite);
    }
    
    public static class JAXRSConfigBuilder extends AbstractContentManagerConfigBuilder<JAXRSConfigBuilder, JAXRSConfig> {
        
        String jaxrsPath = DEFAULT_JAXRS_CONTEXT_PATH;
        String jaxrsHost = DEFAULT_JAXRS_HOST;
        int jaxrsPort = DEFAULT_JAXRS_PORT;
        
        public JAXRSConfigBuilder jaxrsHost(String jaxrsHost) {
            this.jaxrsHost = jaxrsHost;
            return this;
        }
        
        public JAXRSConfigBuilder jaxrsPort(int jaxrsPort) {
            this.jaxrsPort = jaxrsPort;
            return this;
        }
        
        public JAXRSConfigBuilder jaxrsPath(String jaxrsPath) {
            this.jaxrsPath = jaxrsPath;
            return this;
        }
        
        public JAXRSConfig build() {
            return new JAXRSConfig(ipfsAddr, bcImpl, bcUrl, bcHost, bcPort, bcUser, bcPass, ipfsTimeout, ipfsAttempts, ipfsThreads, dataDir, overwrite, jaxrsHost, jaxrsPort, jaxrsPath);
        }
    }
}