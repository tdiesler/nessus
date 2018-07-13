package io.nessus.bitcoin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.Network;
import io.nessus.Wallet;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinBlockchain extends BitcoinClientSupport implements Blockchain {

    static final Logger LOG = LoggerFactory.getLogger(BitcoinBlockchain.class);

    private Wallet wallet;
    private Network network;
    
    public BitcoinBlockchain(BitcoindRpcClient client) {
        super(client);
    }
    
    @Override
    public Wallet getWallet() {
        if (wallet == null) {
            wallet = createWallet();
        }
        return wallet;
    }

    @Override
    public Network getNetwork() {
        if (network == null) {
            network = createNetwork();
        }
        return network;
    }
    
    protected Wallet createWallet() {
        return new BitcoinWallet(this, getRpcClient());
    }

    protected Network createNetwork() {
        return new BitcoinNetwork(this, getRpcClient());
    }
}
