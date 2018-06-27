package io.nessus;

import java.net.URL;


public class BlockchainFactory {

    private static Blockchain INSTANCE;
    
    public static Blockchain getBlockchain(URL rpcUrl) {
        if (INSTANCE == null) {
            try {
                Class<?> clazz = loadImplementaionClass();
                INSTANCE = (Blockchain) clazz.getConstructor(URL.class).newInstance(rpcUrl);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return INSTANCE;
    }

    public static Blockchain getBlockchain(boolean testnet) {
        if (INSTANCE == null) {
            try {
                Class<?> clazz = loadImplementaionClass();
                INSTANCE = (Blockchain) clazz.getConstructor(Boolean.class).newInstance(testnet);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return INSTANCE;
    }
    
    public static Blockchain getBlockchain() {
        return getBlockchain(false);
    }

    private static Class<?> loadImplementaionClass() throws ClassNotFoundException {
        ClassLoader loader = BlockchainFactory.class.getClassLoader();
        return loader.loadClass("io.nessus.bitcoin.BitcoinBlockchain");
    }
}
