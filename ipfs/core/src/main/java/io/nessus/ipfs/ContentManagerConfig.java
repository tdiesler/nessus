package io.nessus.ipfs;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.kohsuke.args4j.Option;

import io.nessus.ipfs.core.AbstractConfig;

public class ContentManagerConfig extends AbstractConfig {

    public static final long DEFAULT_IPFS_TIMEOUT = 6000; // 6 sec
    public static final int DEFAULT_IPFS_ATTEMPTS = 100; // 10 min
    public static final int DEFAULT_IPFS_THREADS = 24;
    
    @Option(name = "--ipfs-timeout", usage = "The maximum number of millis for IPFS operations")
    protected long ipfsTimeout = DEFAULT_IPFS_TIMEOUT;
    
    @Option(name = "--ipfs-attempts", usage = "The max number of IPFS operation attempts")
    protected int ipfsAttempts = DEFAULT_IPFS_ATTEMPTS;
    
    @Option(name = "--ipfs-threads", usage = "The number of threads for IPFS operations")
    protected int ipfsThreads = DEFAULT_IPFS_THREADS;
    
    @Option(name = "--datadir", usage = "The location of the internal data directory")
    protected Path dataDir = Paths.get(System.getProperty("user.home"), ".nessus");
    
    @Option(name = "--overwrite", usage = "Whether to overwrite existing files")
    protected boolean overwrite;

    public ContentManagerConfig() {
    }
    
    protected ContentManagerConfig(String bcImpl, String bcUrl, String bcHost, int bcPort, String bcUser, String bcPass, 
    		String ipfsAddr, long ipfsTimeout, int ipfsAttempts, int ipfsThreads, Path dataDir, boolean overwrite) {
    	super(ipfsAddr, bcImpl, bcUrl, bcHost, bcPort, bcUser, bcPass);
    	
        this.ipfsTimeout = ipfsTimeout;
        this.ipfsAttempts = ipfsAttempts;
        this.ipfsThreads = ipfsThreads;
        this.overwrite = overwrite;
        
        if (dataDir != null)
        	this.dataDir = dataDir;
    }

    public long getIpfsTimeout() {
        return ipfsTimeout;
    }

    public int getIpfsAttempts() {
        return ipfsAttempts;
    }

    public int getIpfsThreads() {
        return ipfsThreads;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public boolean isOverwrite() {
		return overwrite;
	}

    public String toString() {
        return String.format("[dataDir=%s, timeout=%s, attempts=%s, threads=%s, overwrite=%b]", 
                dataDir, ipfsTimeout, ipfsAttempts, ipfsThreads, overwrite);
    }
    
    public static class ContentManagerConfigBuilder extends AbstractContentManagerConfigBuilder<ContentManagerConfigBuilder, ContentManagerConfig>  {
    	
        public ContentManagerConfig build() {
            return new ContentManagerConfig(bcImpl, bcUrl, bcHost, bcPort, bcUser, bcPass, ipfsAddr, ipfsTimeout, ipfsAttempts, ipfsThreads, dataDir, overwrite);
        }
    }
    
    protected static class AbstractContentManagerConfigBuilder<B extends AbstractContentManagerConfigBuilder<?, ?>, C extends ContentManagerConfig> 
    	extends AbstractConfigBuilder<B, C> {
        
        protected long ipfsTimeout = DEFAULT_IPFS_TIMEOUT;
        protected int ipfsAttempts = DEFAULT_IPFS_ATTEMPTS;
        protected int ipfsThreads = DEFAULT_IPFS_THREADS;
        protected boolean overwrite;
        protected Path dataDir;
        
		@SuppressWarnings("unchecked")
		public B datadir(Path dataDir) {
            this.dataDir = dataDir;
            return (B) this;
        }
        
		@SuppressWarnings("unchecked")
		public B overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return (B) this;
        }
        
		@SuppressWarnings("unchecked")
		public B ipfsTimeout(long timeout) {
            this.ipfsTimeout = timeout;
            return (B) this;
        }
        
		@SuppressWarnings("unchecked")
		public B ipfsAttempts(int attempts) {
            this.ipfsAttempts = attempts;
            return (B) this;
        }
        
		@SuppressWarnings("unchecked")
		public B ipfsThreads(int threads) {
            this.ipfsThreads = threads;
            return (B) this;
        }
    }
}