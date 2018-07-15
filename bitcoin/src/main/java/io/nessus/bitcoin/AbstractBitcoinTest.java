package io.nessus.bitcoin;

/*-
 * #%L
 * Nessus :: Bitcoin
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

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;

import io.nessus.AbstractBlockchainTest;
import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.Wallet;

public abstract class AbstractBitcoinTest extends AbstractBlockchainTest {

    @BeforeClass
    public static void beforeClass() throws IOException {

        Blockchain blockchain = BlockchainFactory.getBlockchain(DEFAULT_JSONRPC_REGTEST_URL);
        Wallet wallet = blockchain.getWallet();
        
        importAddresses(wallet);
        
        // Import the configured addresses and generate a few coins
        BigDecimal balanceA = wallet.getBalance("");
        if (balanceA.doubleValue() == 0.0) {

            Network network = blockchain.getNetwork();
            List<String> blocks = network.generate(101, null);
            Assert.assertEquals(101, blocks.size());
        }
    }
}
