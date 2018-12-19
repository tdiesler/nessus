package io.nessus.ipfs;

import java.net.MalformedURLException;
import java.net.URL;

import org.kohsuke.args4j.Option;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.utils.SystemUtils;

public class BlockchainConfig {

    public static final String ENV_BLOCKCHAIN_JSONRPC_ADDR = "BLOCKCHAIN_JSONRPC_ADDR";
    public static final String ENV_BLOCKCHAIN_JSONRPC_PORT = "BLOCKCHAIN_JSONRPC_PORT";
    public static final String ENV_BLOCKCHAIN_JSONRPC_USER = "BLOCKCHAIN_JSONRPC_USER";
    public static final String ENV_BLOCKCHAIN_JSONRPC_PASS = "BLOCKCHAIN_JSONRPC_PASS";

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
    
    @Option(name = "--help", help = true)
    public boolean help;
    
    public BlockchainConfig() {
    }
    
    protected BlockchainConfig(String bcImpl, String bcUrl, String bcHost, int bcPort, String bcUser, String bcPass) {
    	
        this.bcUrl = bcUrl;
        this.bcImpl = bcImpl;
        this.bcHost = bcHost;
        this.bcPort = bcPort;
        this.bcUser = bcUser;
        this.bcPass = bcPass;
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
        ClassLoader loader = BlockchainConfig.class.getClassLoader();
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

    protected static class AbstractConfigBuilder<B extends AbstractConfigBuilder<?, ?>, C extends BlockchainConfig> {
        
        protected String bcUrl = DEFAULT_BLOCKCHAIN_URL;
        protected String bcImpl = DEFAULT_BLOCKCHAIN_IMPL;
        protected String bcHost = DEFAULT_BLOCKCHAIN_HOST;
        protected int bcPort = DEFAULT_BLOCKCHAIN_PORT;
        protected String bcUser = DEFAULT_BLOCKCHAIN_USER;
        protected String bcPass = DEFAULT_BLOCKCHAIN_PASSWORD;
        
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