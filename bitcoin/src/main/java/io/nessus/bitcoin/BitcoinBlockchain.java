package io.nessus.bitcoin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.Network;
import io.nessus.Wallet;

public class BitcoinBlockchain implements Blockchain {

    static final Logger LOG = LoggerFactory.getLogger(BitcoinBlockchain.class);

    private Wallet wallet = new BitcoinWallet(this);
    private Network network = new BitcoinNetwork(this);
    
    @Override
    public Wallet getWallet() {
        return wallet;
    }

    @Override
    public Network getNetwork() {
        return network;
    }

}
