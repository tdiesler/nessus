package io.nessus.ipfs.jaxrs;

import java.net.MalformedURLException;
import java.net.URL;

import org.kohsuke.args4j.Option;

import io.ipfs.multiaddr.MultiAddress;
import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.ipfs.IPFSClient;
import io.nessus.utils.SystemUtils;

public final class JAXRSConfig {

    private static final String DEFAULT_IPFS_ADDR = "/ip4/127.0.0.1/tcp/5001";
    private static final String DEFAULT_BLOCKCHAIN_IMPL = "io.nessus.bitcoin.BitcoinBlockchain";
    private static final String DEFAULT_BLOCKCHAIN_URL = "http://127.0.0.1:18332";
    private static final String DEFAULT_BLOCKCHAIN_HOST = "127.0.0.1";
    private static final int DEFAULT_BLOCKCHAIN_PORT = 18332;
    private static final String DEFAULT_BLOCKCHAIN_USER = "rpcusr";
    private static final String DEFAULT_BLOCKCHAIN_PASSWORD = "rpcpass";
    private static final String DEFAULT_NESSUS_JAXRS_HOST = "127.0.0.1";
    private static final int DEFAULT_NESSUS_JAXRS_PORT = 8081;

    @Option(name = "--ipfs", usage = "The IPFS API address")
    String ipfsAddr = DEFAULT_IPFS_ADDR;

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
    
    @Option(name = "--host", usage = "The Nessus JAXRS host")
    String jaxrsHost = DEFAULT_NESSUS_JAXRS_HOST;

    @Option(name = "--port", usage = "The Nessus JAXRS port")
    int jaxrsPort = DEFAULT_NESSUS_JAXRS_PORT;

    public JAXRSConfig() {
    }
    
    private JAXRSConfig(String ipfsAddr, String bcImpl, String bcUrl, String bcHost, int bcPort, String bcUser, String bcPass, String jaxrsHost, int jaxrsPort) {
        this.ipfsAddr = ipfsAddr;
        this.bcUrl = bcUrl;
        this.bcImpl = bcImpl;
        this.bcHost = bcHost;
        this.bcPort = bcPort;
        this.bcUser = bcUser;
        this.bcPass = bcPass;
        this.jaxrsHost = jaxrsHost;
        this.jaxrsPort = jaxrsPort;
    }

    public MultiAddress getIpfsAddress() {
        if (DEFAULT_IPFS_ADDR.equals(ipfsAddr)) {
            String host = SystemUtils.getenv(IPFSClient.ENV_IPFS_JSONRPC_ADDR, "127.0.0.1");
            String port = SystemUtils.getenv(IPFSClient.ENV_IPFS_JSONRPC_PORT, "5001");
            ipfsAddr = String.format("/ip4/%s/tcp/%s", host, port);
        }
        return new MultiAddress(ipfsAddr);
    }
    
    public URL getBlockchainUrl() throws MalformedURLException {
        
        if (!DEFAULT_BLOCKCHAIN_URL.equals(bcUrl)) 
            return new URL(bcUrl);
        
        if (DEFAULT_BLOCKCHAIN_HOST.equals(bcHost))
            bcHost = SystemUtils.getenv(JAXRSConstants.ENV_BLOCKCHAIN_JSONRPC_ADDR, bcHost);
        
        if (DEFAULT_BLOCKCHAIN_PORT == bcPort)
            bcPort = Integer.parseInt(SystemUtils.getenv(JAXRSConstants.ENV_BLOCKCHAIN_JSONRPC_PORT, "" + bcPort));
        
        if (DEFAULT_BLOCKCHAIN_USER.equals(bcUser))
            bcUser = SystemUtils.getenv(JAXRSConstants.ENV_BLOCKCHAIN_JSONRPC_USER, bcUser);
        
        if (DEFAULT_BLOCKCHAIN_PASSWORD.equals(bcPass))
            bcPass = SystemUtils.getenv(JAXRSConstants.ENV_BLOCKCHAIN_JSONRPC_PASS, bcPass);
        
        return new URL(String.format("http://%s:%s@%s:%s", bcUser, bcPass, bcHost, bcPort));   
    }
    
    @SuppressWarnings("unchecked")
    public Class<Blockchain> getBlockchainImpl() throws ClassNotFoundException {
        if (DEFAULT_BLOCKCHAIN_IMPL.equals(bcImpl)) {
            bcImpl = SystemUtils.getenv(BlockchainFactory.BLOCKCHAIN_CLASS_NAME, bcImpl);
        }
        ClassLoader loader = JAXRSConfig.class.getClassLoader();
        return (Class<Blockchain>) loader.loadClass(bcImpl);
    }

    public URL getJaxrsUrl() throws MalformedURLException {
        
        if (DEFAULT_NESSUS_JAXRS_HOST.equals(bcHost))
            jaxrsHost = SystemUtils.getenv(JAXRSConstants.ENV_NESSUS_JAXRS_ADDR, jaxrsHost);
        
        if (DEFAULT_NESSUS_JAXRS_PORT == jaxrsPort)
            jaxrsPort = Integer.parseInt(SystemUtils.getenv(JAXRSConstants.ENV_NESSUS_JAXRS_PORT, "" + jaxrsPort));
        
        return new URL(String.format("http://%s:%s/nessus", jaxrsHost, jaxrsPort));   
    }
    
    public static class Builder {
        
        String ipfsAddr = DEFAULT_IPFS_ADDR;
        String bcUrl = DEFAULT_BLOCKCHAIN_URL;
        String bcImpl = DEFAULT_BLOCKCHAIN_IMPL;
        String bcHost = DEFAULT_BLOCKCHAIN_HOST;
        int bcPort = DEFAULT_BLOCKCHAIN_PORT;
        String bcUser = DEFAULT_BLOCKCHAIN_USER;
        String bcPass = DEFAULT_BLOCKCHAIN_PASSWORD;
        String jaxrsHost = DEFAULT_NESSUS_JAXRS_HOST;
        int jaxrsPort = DEFAULT_NESSUS_JAXRS_PORT;
        
        public Builder ipfs(String addr) {
            this.ipfsAddr = addr;
            return this;
        }
        
        public Builder bcimpl(Class<Blockchain> impl) {
            this.bcImpl = impl.getName();
            return this;
        }
        
        public Builder bcurl(String bcUrl) {
            this.bcUrl = bcUrl;
            return this;
        }
        
        public Builder bchost(String bcHost) {
            this.bcHost = bcHost;
            return this;
        }
        
        public Builder bcport(int bcPort) {
            this.bcPort = bcPort;
            return this;
        }
        
        public Builder bcuser(String bcUser) {
            this.bcUser = bcUser;
            return this;
        }
        
        public Builder bcpass(String bcPass) {
            this.bcPass = bcPass;
            return this;
        }
        
        public Builder host(String jaxrsHost) {
            this.jaxrsHost = jaxrsHost;
            return this;
        }
        
        public Builder port(int jaxrsPort) {
            this.jaxrsPort = jaxrsPort;
            return this;
        }
        
        public JAXRSConfig build() {
            return new JAXRSConfig(ipfsAddr, bcImpl, bcUrl, bcHost, bcPort, bcUser, bcPass, jaxrsHost, jaxrsPort);
        }
    }
}