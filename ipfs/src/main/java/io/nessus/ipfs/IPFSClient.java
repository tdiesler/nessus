package io.nessus.ipfs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.AssertState;

public class IPFSClient extends AbstractClient {

    public static final String ENV_IPFS_API_HOST = "AEG_IPFS_API_HOST";
    public static final String ENV_IPFS_API_PORT = "AEG_IPFS_API_PORT";
    
    public static final String ENV_IPFS_GATEWAY_HOST = "AEG_IPFS_GATEWAY_HOST";
    public static final String ENV_IPFS_GATEWAY_PORT = "AEG_IPFS_GATEWAY_PORT";
    
    public static final String ENV_IPFS_WEBUI_HOST = "AEG_IPFS_WEBUI_HOST";
    public static final String ENV_IPFS_WEBUI_PORT = "AEG_IPFS_WEBUI_PORT";
    
    static final Logger LOG = LoggerFactory.getLogger(IPFSClient.class);
    
    final String apiopt;
    
    public IPFSClient() {
        this(10L, TimeUnit.SECONDS);
    }
    
    public IPFSClient(Long timeout, TimeUnit unit) {
        super(timeout, unit);
        
        String host = System.getenv(ENV_IPFS_API_HOST);
        if (host != null) {
            String port = System.getenv(ENV_IPFS_API_PORT);
            port = port != null ? port : "5001";
            apiopt = String.format("--api=/ip4/%s/tcp/%s", host, port);
        } else {
            apiopt = null;
        }
    }

    public String add(String... args) {
        String[] cmd = concat("add", opts(), args); 
        String res = exec(ipfs(), cmd);
        String[] toks = split(res);
        AssertState.assertEquals("added", toks[0]);
        return toks[1];
    }

    public String cat(String... args) {
        String[] cmd = concat("cat", opts(), args); 
        return exec(ipfs(), cmd);
    }

    public String get(String... args) {
        String[] cmd = concat("get", opts(), args); 
        return exec(ipfs(), cmd);
    }

    public String version() {
        String[] cmd = concat("version", opts()); 
        return exec(ipfs(), cmd);
    }

    private String[] opts(String... opts) {
        List<String> list = new ArrayList<>();
        if (apiopt != null) {
            list.add(apiopt);
        }
        if (opts != null) {
            list.addAll(Arrays.asList(opts));
        }
        return list.toArray(new String[list.size()]);
    }

    private String ipfs() {
        return "ipfs";
    }
}
