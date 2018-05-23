package io.nessus;

import java.util.List;

public interface Account {

    /**
     * Get the account name
     */
    String getName();

    /**
     * Get the private key associated with this account
     */
    String getPrivKey();

    /**
     * Get the prmary address for this account
     */
    String getPrimaryAddress();

    /**
     * Get the list of known addresses for this account
     */
    List<String> getAddresses();
}