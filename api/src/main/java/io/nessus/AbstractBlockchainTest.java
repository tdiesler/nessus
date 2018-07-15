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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;

public abstract class AbstractBlockchainTest {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    public static final String LABEL_BOB = "Bob";
    public static final String LABEL_MARRY = "Marry";
    public static final String LABEL_SINK = "Sink";
    
    protected static void importAddresses(Wallet wallet) throws IOException {
        
        // Wallet already initialized
        List<String> labels = wallet.getLabels();
        if (!labels.isEmpty()) return;
        
        Config config = Config.parseConfig("/initial-import.json");
        for (Config.Address addr : config.getWallet().getAddresses()) {
            String privKey = addr.getPrivKey();
            String pubKey = addr.getPubKey();
            try {
                if (privKey != null && pubKey == null) {
                    wallet.addPrivateKey(privKey, addr.getLabels());
                } else {
                    wallet.addAddress(pubKey, addr.getLabels());
                }
            } catch (BitcoinRPCException ex) {
                String message = ex.getMessage();
                if (!message.contains("walletpassphrase")) throw ex;
            }
        }
    }
    
    protected BigDecimal estimateFee() {
        Blockchain blockchain = BlockchainFactory.getBlockchain();
        return blockchain.getNetwork().estimateFee();
    }
    
    protected BigDecimal addFee(BigDecimal amount) {
        return amount.add(estimateFee());
    }
    
    protected BigDecimal subtractFee(BigDecimal amount) {
        return amount.subtract(estimateFee());
    }
    
    protected BigDecimal getUTXOAmount(List<UTXO> utxos) {
        BigDecimal result = BigDecimal.ZERO;
        for (UTXO utxo : utxos) {
            result = result.add(utxo.getAmount());
        }
        return result;
    }
    
    protected void showAccountBalances() {
        Blockchain blockchain = BlockchainFactory.getBlockchain();
        Wallet wallet = blockchain.getWallet();
        for (String label : wallet.getLabels()) {
            if (!label.startsWith("_")) {
                BigDecimal val = wallet.getBalance(label);
                LOG.info(String.format("%-5s: %13.8f", label, val));
            }
        }
    }
}
