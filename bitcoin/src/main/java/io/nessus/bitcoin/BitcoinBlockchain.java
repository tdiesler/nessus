package io.nessus.bitcoin;

import io.nessus.AbstractBlockchain;
import io.nessus.AbstractNetwork;
import io.nessus.AbstractWallet;
import io.nessus.Blockchain;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinBlockchain extends AbstractBlockchain implements Blockchain {

    public BitcoinBlockchain(BitcoindRpcClient client) {
        super(client);
    }
    
    @Override
    protected AbstractWallet createWallet() {
        return new BitcoinWallet(this, getRpcClient());
    }

    @Override
    protected AbstractNetwork createNetwork() {
        return new BitcoinNetwork(this, getRpcClient());
    }
}
