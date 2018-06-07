package io.nessus;

import java.math.BigDecimal;
import java.util.List;

import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.Unspent;

public interface Wallet {

    /**
     * Add a private key to this wallet
     */
    Address addPrivateKey(String privKey, List<String> labels);
    
    /**
     * Add a watch only address to this wallet
     */
    Address addAddress(String key, List<String> labels);
    
    /**
     * List available label
     */
    List<String> getLabels();

    /**
     * Get the default address for a given label
     */
    Address getDefaultAddress(String label);

    /**
     * Get the address obj for address string
     */
    Address getAddress(String address);

    /**
     * Get the addresses for a given label
     */
    List<Address> getAddresses(String label);

    /**
     * List UTOXs associated with a list of addresses
     */
    List<Unspent> listUnspent(List<String> addrs);
    
    /**
     * Get the balance for a given account
     */
    BigDecimal getBalance(String account);

    /**
     * Sends funds from the default account to an address 
     * @return The tranaction id
     */
    String sendToAddress(String toAddress, BigDecimal amount);

    /**
     * Sends funds that are associated with a given label to an address
     * @return The tranaction id
     */
    String sendFromLabel(String label, String toAddress, BigDecimal amount);

    interface Address {
        
        String getPrivKey();
        
        String getAddress();
        
        boolean isWatchOnly();
        
        void addLabel(String label);

        void removeLabel(String label);
        
        List<String> getLabels();
    }
}
