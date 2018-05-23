package io.nessus;

import java.math.BigDecimal;
import java.util.List;

import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.Unspent;

public interface Wallet {

    /**
     * Import a private key 
     * @param privKey
     * @param account
     */
    void createAccount(String account, String privKey);

    /**
     * List available account names
     */
    List<String> getAccountNames();

    /**
     * Get the account for the given name
     */
    Account getAccount(String name);

    /**
     * Add an account with a given name and address
     */
    Account addAccount(String name, String address);
    
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
     * Sends funds from a given account to an address
     * @return The tranaction id
     */
    String sendFromAccount(String account, String toAddress, BigDecimal amount);
}
