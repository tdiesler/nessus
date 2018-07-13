package io.nessus.bitcoin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public abstract class BitcoinClientSupport {

    static final Logger LOG = LoggerFactory.getLogger(BitcoinClientSupport.class);

    protected final BitcoindRpcClient client;
    
    public BitcoinClientSupport(BitcoindRpcClient client) {
        this.client = client;
    }
    
    public BitcoindRpcClient getRpcClient() {
        return client;
    }
}
