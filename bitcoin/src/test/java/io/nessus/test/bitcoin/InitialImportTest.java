package io.nessus.test.bitcoin;

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

import static io.nessus.Wallet.LABEL_CHANGE;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.bitcoin.AbstractBitcoinTest;

public class InitialImportTest extends AbstractBitcoinTest {

    @Test
    public void testInitialImport () throws Exception {

        Blockchain blockchain = BlockchainFactory.getBlockchain();
        Wallet wallet = blockchain.getWallet();
        
        List<String> labels = wallet.getLabels();
        Assert.assertEquals(Arrays.asList(LABEL_BOB, LABEL_MARRY, LABEL_SINK, LABEL_CHANGE), labels);
        
        for (String label : labels) {
            List<Address> addrs = wallet.getAddresses(label);
            LOG.info(String.format("%-5s: addr=%s", label, addrs));
            Assert.assertTrue("At least one address", addrs.size() > 0);
        }
    }
}
