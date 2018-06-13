package io.nessus;

import java.math.BigDecimal;
import java.util.List;

public interface Wallet {

    static final String LABEL_DEFAULT = "_default";
    static final String LABEL_CHANGE = "_change";
    
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
    Address getAddress(String label);

    /**
     * Get addresses for a given label
     */
    List<Address> getAddresses(String label);

    /**
     * Get addresses for a given label
     */
    List<String> getRawAddresses(String label);

    /**
     * Get the default change address for a given label
     */
    Address getChangeAddress(String label);

    /**
     * Get change addresses for a given label
     */
    List<Address> getChangeAddresses(String label);

    /**
     * Get change addresses for a given label
     */
    List<String> getRawChangeAddresses(String label);

    /**
     * Get the balance for a given account
     */
    BigDecimal getBalance(String account);

    /**
     * Sends funds from the default account to an address 
     * @return The transaction id
     */
    String sendToAddress(String toAddress, BigDecimal amount);

    /**
     * Sends funds that are associated with a given label to an address
     * @return The transaction id
     */
    String sendFromLabel(String label, String toAddress, BigDecimal amount);

    /**
     * Send a raw transaction to the network
     * @return The transaction id
     */
    String sendTx(Tx tx);

    /**
     * Select a list of unspent transaction outputs that sattisfy the requested amount
     */
    List<UTXO> selectUnspent(String label, BigDecimal amount);

    /**
     * Get all unspent transaction outputs associated with the given label
     */
    List<UTXO> listUnspent(String label);
    
    /**
     * List UTOXs associated with a list of addresses
     */
    List<UTXO> listUnspent(List<String> addrs);
    
    interface Address {
        
        String getPrivKey();
        
        String getAddress();
        
        boolean isWatchOnly();
        
        void addLabel(String label);

        void removeLabel(String label);
        
        List<String> getLabels();
    }
}
