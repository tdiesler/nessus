package io.nessus;

/*-
 * #%L
 * Nessus :: API
 * %%
 * Copyright (C) 2018 Nessus
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.math.BigDecimal;
import java.util.List;

public interface Wallet {

    static final String LABEL_CHANGE = "(change)";
    static final BigDecimal ALL_FUNDS = new BigDecimal(Integer.MIN_VALUE);
    
    /**
     * Import addressses from configuration.
     */
    void importAddresses(Config config);
    
    /**
     * Add an address deriven from a private key to this wallet
     */
    Address addPrivateKey(String privKey, List<String> labels);
    
    /**
     * Add a watch only address to this wallet
     */
    Address addAddress(String key, List<String> labels);
    
    /**
     * Generate new address.
     */
    Address newAddress(String label);
    
    /**
     * Generate new change address.
     */
    Address newChangeAddress(String label);
    
    /**
     * List available label
     */
    List<String> getLabels();

    /**
     * Get all addresses.
     */
    List<Address> getAddresses();

    /**
     * Get the default address for a given label
     */
    Address getAddress(String label);

    /**
     * Find the address for a given raw address
     */
    Address findAddress(String rawAddr);

    /**
     * Get addresses for a given label.
     */
    List<Address> getAddresses(String label);

    /**
     * Get a random change address for a given label.
     * 
     * If there is no change address yet, it creates one. 
     */
    Address getChangeAddress(String label);

    /**
     * Get change addresses for a given label
     */
    List<Address> getChangeAddresses(String label);

    /**
     * Get the balance for a given label
     */
    BigDecimal getBalance(String label);

    /**
     * Get the balance for a given address
     */
    BigDecimal getBalance(Address addr);

    /**
     * Sends funds from the default account to an address 
     * @return The transaction id
     */
    String sendToAddress(String toAddr, BigDecimal amount);

    /**
     * Sends funds that are associated with a given label to an address
     * @return The transaction id
     */
    String sendFromLabel(String label, String toAddr, BigDecimal amount);

    /**
     * Send a raw transaction to the network
     * @return The transaction id
     */
    String sendTx(Tx tx);

    /**
     * Select a list of unspent transaction outputs that satisfy the requested amount
     */
    List<UTXO> selectUnspent(String label, BigDecimal amount);

    /**
     * Select a list of unspent transaction outputs that satisfy the requested amount
     */
    List<UTXO> selectUnspent(List<Address> addrs, BigDecimal amount);

    /**
     * Get all unspent transaction outputs associated with the given label
     */
    List<UTXO> listUnspent(String label);
    
    /**
     * List UTXOs associated with a list of addresses
     */
    List<UTXO> listUnspent(List<Address> addrs);
    
    /**
     * List locked UTXOs associated with a list of addresses
     * 
     * The lock state of an UTXO is delegated to the underlying 
     * wallet implementation and may not survive wallet restart. 
     */
    List<UTXO> listLockUnspent(List<Address> addrs);
    
    /**
     * Lock an unspent transaction output.
     * 
     * The lock state of an UTXO is delegated to the underlying 
     * wallet implementation and may not survive wallet restart. 
     */
    boolean lockUnspent(UTXO utxo, boolean unlock);
    
    /**
     * Get the transaction for the given Id
     */
    Tx getTransaction(String txId);
    
    interface Address {
        
        String getPrivKey();
        
        String getAddress();
        
        boolean isWatchOnly();
        
        List<String> getLabels();
        
        Address setLabels(List<String> labels);
    }
}
