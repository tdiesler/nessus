package io.nessus;

import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public abstract class RpcClientSupport {

    protected final BitcoindRpcClient client;
    
    public RpcClientSupport(BitcoindRpcClient client) {
        this.client = client;
    }
    
    public BitcoindRpcClient getRpcClient() {
        return client;
    }
}
