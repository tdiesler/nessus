package io.nessus.ipfs;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.kohsuke.args4j.Option;

import io.ipfs.multiaddr.MultiAddress;
import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.ipfs.core.DefaultIPFSClient;
import io.nessus.utils.SystemUtils;

public class Config {

    public static final String ENV_IPFS_JSONRPC_ADDR = "IPFS_JSONRPC_ADDR";
    public static final String ENV_IPFS_JSONRPC_PORT = "IPFS_JSONRPC_PORT";
    
    public static final String ENV_IPFS_GATEWAY_ADDR = "IPFS_GATEWAY_ADDR";
    public static final String ENV_IPFS_GATEWAY_PORT = "IPFS_GATEWAY_PORT";
    
    public static final String ENV_BLOCKCHAIN_JSONRPC_ADDR = "BLOCKCHAIN_JSONRPC_ADDR";
    public static final String ENV_BLOCKCHAIN_JSONRPC_PORT = "BLOCKCHAIN_JSONRPC_PORT";
    public static final String ENV_BLOCKCHAIN_JSONRPC_USER = "BLOCKCHAIN_JSONRPC_USER";
    public static final String ENV_BLOCKCHAIN_JSONRPC_PASS = "BLOCKCHAIN_JSONRPC_PASS";

    private static final String DEFAULT_IPFS_ADDR = "/ip4/127.0.0.1/tcp/5001";
    
    private static final String DEFAULT_BLOCKCHAIN_IMPL = "io.nessus.bitcoin.BitcoinBlockchain";
    
    private static final String DEFAULT_BLOCKCHAIN_URL = "http://127.0.0.1:18332";
    private static final String DEFAULT_BLOCKCHAIN_HOST = "127.0.0.1";
    private static final int DEFAULT_BLOCKCHAIN_PORT = 18332;
    private static final String DEFAULT_BLOCKCHAIN_USER = "rpcusr";
    private static final String DEFAULT_BLOCKCHAIN_PASSWORD = "rpcpass";
    
    @Option(name = "--bcimpl", usage = "The Blockchain implementation class")
    String bcImpl = DEFAULT_BLOCKCHAIN_IMPL;

    @Option(name = "--bcurl", forbids = { "--bchost", "--bcport" }, usage = "The Blockchain RPC URL")
    String bcUrl = DEFAULT_BLOCKCHAIN_URL;

    @Option(name = "--bchost", forbids = { "--bcurl" }, usage = "The Blockchain RPC host")
    String bcHost = DEFAULT_BLOCKCHAIN_HOST;

    @Option(name = "--bcport", forbids = { "--bcurl" }, usage = "The Blockchain RPC port")
    int bcPort = DEFAULT_BLOCKCHAIN_PORT;

    @Option(name = "--bcuser", usage = "The Blockchain RPC user")
    String bcUser = DEFAULT_BLOCKCHAIN_USER;
    
    @Option(name = "--bcpass", usage = "The Blockchain RPC password")
    String bcPass = DEFAULT_BLOCKCHAIN_PASSWORD;
    
    @Option(name = "--ipfs", usage = "The IPFS API address")
    String ipfsAddr = DEFAULT_IPFS_ADDR;

    @Option(name = "--ipfs-timeout", usage = "The maximum number of millis for IPFS operations")
    long ipfsTimeout = ContentManager.DEFAULT_IPFS_TIMEOUT;
    
    @Option(name = "--ipfs-attempts", usage = "The max number of IPFS operation attempts")
    int ipfsAttempts = ContentManager.DEFAULT_IPFS_ATTEMPTS;
    
    @Option(name = "--ipfs-threads", usage = "The number of threads for IPFS operations")
    int ipfsThreads = ContentManager.DEFAULT_IPFS_THREADS;
    
    @Option(name = "--datadir", usage = "The location of the internal data directory")
    Path dataDir = Paths.get(System.getProperty("user.home"), ".nessus");
    
    @Option(name = "--overwrite", usage = "Whether to overwrite existing files")
    boolean overwrite;

    public Config() {
    }
    
    protected Config(String ipfsAddr, long ipfsTimeout, int ipfsAttempts, int ipfsThreads, String bcImpl, String bcUrl,
    		String bcHost, int bcPort, String bcUser, String bcPass, Path dataDir, boolean overwrite) {
    	
        this.ipfsAddr = ipfsAddr;
        this.ipfsTimeout = ipfsTimeout;
        this.ipfsAttempts = ipfsAttempts;
        this.ipfsThreads = ipfsThreads;
        this.bcUrl = bcUrl;
        this.bcImpl = bcImpl;
        this.bcHost = bcHost;
        this.bcPort = bcPort;
        this.bcUser = bcUser;
        this.bcPass = bcPass;
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

	public MultiAddress getIpfsAddress() {
        if (DEFAULT_IPFS_ADDR.equals(ipfsAddr)) {
            String host = SystemUtils.getenv(ENV_IPFS_JSONRPC_ADDR, "127.0.0.1");
            String port = SystemUtils.getenv(ENV_IPFS_JSONRPC_PORT, "5001");
            ipfsAddr = String.format("/ip4/%s/tcp/%s", host, port);
        }
        return new MultiAddress(ipfsAddr);
    }
    
	public IPFSClient getIpfsClient () {
        MultiAddress ipfsAddr = getIpfsAddress();
        IPFSClient ipfsClient = new DefaultIPFSClient(ipfsAddr);
        return ipfsClient;
	}
	
    public URL getBlockchainUrl() throws MalformedURLException {
        
        if (!DEFAULT_BLOCKCHAIN_URL.equals(bcUrl)) 
            return new URL(bcUrl);
        
        if (DEFAULT_BLOCKCHAIN_HOST.equals(bcHost))
            bcHost = SystemUtils.getenv(ENV_BLOCKCHAIN_JSONRPC_ADDR, bcHost);
        
        if (DEFAULT_BLOCKCHAIN_PORT == bcPort)
            bcPort = Integer.parseInt(SystemUtils.getenv(ENV_BLOCKCHAIN_JSONRPC_PORT, "" + bcPort));
        
        if (DEFAULT_BLOCKCHAIN_USER.equals(bcUser))
            bcUser = SystemUtils.getenv(ENV_BLOCKCHAIN_JSONRPC_USER, bcUser);
        
        if (DEFAULT_BLOCKCHAIN_PASSWORD.equals(bcPass))
            bcPass = SystemUtils.getenv(ENV_BLOCKCHAIN_JSONRPC_PASS, bcPass);
        
        return new URL(String.format("http://%s:%s@%s:%s", bcUser, bcPass, bcHost, bcPort));   
    }
    
    @SuppressWarnings("unchecked")
    public Class<Blockchain> getBlockchainImpl() throws ClassNotFoundException {
        if (DEFAULT_BLOCKCHAIN_IMPL.equals(bcImpl)) {
            bcImpl = SystemUtils.getenv(BlockchainFactory.BLOCKCHAIN_CLASS_NAME, bcImpl);
        }
        ClassLoader loader = Config.class.getClassLoader();
        return (Class<Blockchain>) loader.loadClass(bcImpl);
    }
    
    public Blockchain getBlockchain() {
        Blockchain blockchain;
		try {
			URL bcUrl = getBlockchainUrl();
			Class<Blockchain> bcImpl = getBlockchainImpl();
			blockchain = BlockchainFactory.getBlockchain(bcUrl, bcImpl);
		} catch (ClassNotFoundException | MalformedURLException ex) {
			throw new IllegalStateException(ex);
		}
        return blockchain;
    }

    public String toString() {
        return String.format("[dataDir=%s, timeout=%s, attempts=%s, threads=%s, overwrite=%b]", 
                dataDir, ipfsTimeout, ipfsAttempts, ipfsThreads, overwrite);
    }
    
    public static class ConfigBuilder extends AbstractBuilder<ConfigBuilder, Config> {
    	
        public Config build() {
            return new Config(ipfsAddr, ipfsTimeout, ipfsAttempts, ipfsThreads, bcImpl, bcUrl, bcHost, bcPort, bcUser, bcPass, dataDir, overwrite);
        }
    }
    
    public static class AbstractBuilder<B extends AbstractBuilder<?, ?>, C extends Config> {
        
        protected String ipfsAddr = DEFAULT_IPFS_ADDR;
        protected String bcUrl = DEFAULT_BLOCKCHAIN_URL;
        protected String bcImpl = DEFAULT_BLOCKCHAIN_IMPL;
        protected String bcHost = DEFAULT_BLOCKCHAIN_HOST;
        protected int bcPort = DEFAULT_BLOCKCHAIN_PORT;
        protected String bcUser = DEFAULT_BLOCKCHAIN_USER;
        protected String bcPass = DEFAULT_BLOCKCHAIN_PASSWORD;
        protected long ipfsTimeout = ContentManager.DEFAULT_IPFS_TIMEOUT;
        protected int ipfsAttempts = ContentManager.DEFAULT_IPFS_ATTEMPTS;
        protected int ipfsThreads = ContentManager.DEFAULT_IPFS_THREADS;
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
		public B ipfsAddr(String addr) {
            this.ipfsAddr = addr;
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
        
        @SuppressWarnings("unchecked")
        public B bcimpl(Class<Blockchain> impl) {
            this.bcImpl = impl.getName();
            return (B) this;
        }
        
        @SuppressWarnings("unchecked")
        public B bcurl(URL bcUrl) {
            this.bcUrl = bcUrl.toString();
            return (B) this;
        }
        
        @SuppressWarnings("unchecked")
        public B bchost(String bcHost) {
            this.bcHost = bcHost;
            return (B) this;
        }
        
        @SuppressWarnings("unchecked")
        public B bcport(int bcPort) {
            this.bcPort = bcPort;
            return (B) this;
        }
        
        @SuppressWarnings("unchecked")
        public B bcuser(String bcUser) {
            this.bcUser = bcUser;
            return (B) this;
        }
        
        @SuppressWarnings("unchecked")
        public B bcpass(String bcPass) {
            this.bcPass = bcPass;
            return (B) this;
        }
    }
}