package io.nessus;

import java.net.URL;

import io.nessus.bitcoin.BitcoinBlockchain;

public class BlockchainFactory {

    private static Blockchain INSTANCE;
    
    public static Blockchain getBlockchain(URL rpcUrl) {
        if (INSTANCE == null) {
            INSTANCE = new BitcoinBlockchain(rpcUrl);
        }
        return INSTANCE;
    }
    
    public static Blockchain getBlockchain(boolean testnet) {
        if (INSTANCE == null) {
            INSTANCE = new BitcoinBlockchain(testnet);
        }
        return INSTANCE;
    }
    
    public static Blockchain getBlockchain() {
        return getBlockchain(false);
    }
}
