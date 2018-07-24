package io.nessus.testing;

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

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Config;
import io.nessus.Network;
import io.nessus.UTXO;
import io.nessus.Wallet;

public abstract class AbstractBlockchainTest {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    public static final String LABEL_BOB = "Bob";
    public static final String LABEL_MARRY = "Marry";
    public static final String LABEL_SINK = "Sink";
    
    protected static void importAddresses(Wallet wallet) throws IOException {
        
        Config config = Config.parseConfig("/initial-import.json");
        if (config != null) wallet.importAddresses(config);
    }
    
    protected static void generate(Blockchain blockchain) {
        Wallet wallet = blockchain.getWallet();
        BigDecimal balance = wallet.getBalance("");
        if (balance.doubleValue() == 0.0) {
            Network network = blockchain.getNetwork();
            List<String> blocks = network.generate(101, null);
            Assert.assertEquals(101, blocks.size());
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
            if (!label.equals(Wallet.LABEL_CHANGE)) {
                BigDecimal val = wallet.getBalance(label);
                LOG.info(String.format("%-5s: %13.8f", label, val));
            }
        }
    }
}
