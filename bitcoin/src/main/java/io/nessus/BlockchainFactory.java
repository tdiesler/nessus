package io.nessus;

import io.nessus.bitcoin.BitcoinBlockchain;

public class BlockchainFactory {

    private static Blockchain INSTANCE = new BitcoinBlockchain();
    
    public static Blockchain getBlockchain() {
        return INSTANCE;
    }
}
