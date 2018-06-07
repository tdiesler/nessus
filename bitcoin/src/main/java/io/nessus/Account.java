package io.nessus;

import java.util.List;

@Deprecated
public interface Account {

    /**
     * Get the account name
     */
    String getName();

    /**
     * Import a private key associated with this account.
     * 
     * The private key is passed on the underlying wallet implementation 
     * and not stored otherwise.
     * 
     * @return The associated public key
     */
    String importPrivKey(String privKey);
    
    /**
     * Get the private key associated with the given address
     * 
     * The private key is obtained from the underlying wallet implementation 
     * which must be able to maintain the pub/priv key association.
     */
    String getPrivKey(String address);
    
    /**
     * Get the default address for this account
     */
    String getDefaultAddress();

    /**
     * Get the list of known addresses for this account
     */
    List<String> getAddresses();

    /**
     * Get a new address for this account
     */
    String getNewAddress();

}