package io.nessus.ipfs.jaxrs;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import org.kohsuke.args4j.Option;

import io.nessus.ipfs.Config;
import io.nessus.utils.SystemUtils;

public final class JAXRSConfig extends Config {

    private static final String DEFAULT_NESSUS_JAXRS_HOST = "0.0.0.0";
    private static final int DEFAULT_NESSUS_JAXRS_PORT = 8081;

    @Option(name = "--host", usage = "The Nessus JAXRS host")
    String jaxrsHost = DEFAULT_NESSUS_JAXRS_HOST;

    @Option(name = "--port", usage = "The Nessus JAXRS port")
    int jaxrsPort = DEFAULT_NESSUS_JAXRS_PORT;

    public JAXRSConfig() {
    }
    
    private JAXRSConfig(String ipfsAddr, long ipfsTimeout, int ipfsAttempts, int ipfsThreads, String bcImpl, String bcUrl, 
    		String bcHost, int bcPort, String bcUser, String bcPass, String jaxrsHost, int jaxrsPort, Path dataDir, boolean overwrite) {
    	super(ipfsAddr, ipfsTimeout, ipfsAttempts, ipfsThreads, bcImpl, bcUrl, bcHost, bcPort, bcUser, bcPass, dataDir, overwrite);
        this.jaxrsHost = jaxrsHost;
        this.jaxrsPort = jaxrsPort;
    }

    public URL getJaxrsUrl() throws MalformedURLException {
        
        if (DEFAULT_NESSUS_JAXRS_HOST.equals(jaxrsHost))
            jaxrsHost = SystemUtils.getenv(JAXRSConstants.ENV_NESSUS_JAXRS_ADDR, jaxrsHost);
        
        if (DEFAULT_NESSUS_JAXRS_PORT == jaxrsPort)
            jaxrsPort = Integer.parseInt(SystemUtils.getenv(JAXRSConstants.ENV_NESSUS_JAXRS_PORT, "" + jaxrsPort));
        
        return new URL(String.format("http://%s:%s/nessus", jaxrsHost, jaxrsPort));   
    }
    
    public static class Builder extends AbstractBuilder<Builder, JAXRSConfig> {
        
        String jaxrsHost = DEFAULT_NESSUS_JAXRS_HOST;
        int jaxrsPort = DEFAULT_NESSUS_JAXRS_PORT;
        
        public Builder jaxrsHost(String jaxrsHost) {
            this.jaxrsHost = jaxrsHost;
            return this;
        }
        
        public Builder jaxrsPort(int jaxrsPort) {
            this.jaxrsPort = jaxrsPort;
            return this;
        }
        
        public JAXRSConfig build() {
            return new JAXRSConfig(ipfsAddr, ipfsTimeout, ipfsAttempts, ipfsThreads, bcImpl, bcUrl, bcHost, bcPort, bcUser, bcPass, jaxrsHost, jaxrsPort, dataDir, overwrite);
        }
    }
}