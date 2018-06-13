package io.nessus.bitcoin;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.Network;
import io.nessus.Wallet;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinBlockchain implements Blockchain {

    static final Logger LOG = LoggerFactory.getLogger(BitcoinBlockchain.class);

    private final BitcoindRpcClient client;
    private final Wallet wallet;
    private final Network network;
    
    public BitcoinBlockchain(URL rpcUrl) {
        client = new BitcoinJSONRPCClient(rpcUrl);
        wallet = new BitcoinWallet(this, client);
        network = new BitcoinNetwork(this, client);
    }
    
    public BitcoinBlockchain(boolean testnet) {
        client = new BitcoinJSONRPCClient(testnet);
        wallet = new BitcoinWallet(this, client);
        network = new BitcoinNetwork(this, client);
    }
    
    public BitcoindRpcClient getClient() {
        return client;
    }

    @Override
    public Wallet getWallet() {
        return wallet;
    }

    @Override
    public Network getNetwork() {
        return network;
    }

}
