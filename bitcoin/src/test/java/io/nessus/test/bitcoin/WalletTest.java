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

import static io.nessus.Wallet.ALL_FUNDS;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.bitcoin.AbstractBitcoinTest;

public class WalletTest extends AbstractBitcoinTest {

    @Test
    public void testInitialImport () throws Exception {
        
        Blockchain blockchain = BlockchainFactory.getBlockchain();
        Wallet wallet = blockchain.getWallet();

        List<String> labels = wallet.getLabels();
        Assert.assertEquals("[(change), Bob, Marry, Sink]", labels.toString());
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        Assert.assertNotNull(wallet.getChangeAddress(LABEL_BOB));
        Assert.assertNotNull(addrBob);
        
        Address addrMarry = wallet.getAddress(LABEL_MARRY);
        Assert.assertNotNull(wallet.getChangeAddress(LABEL_MARRY));
        Assert.assertNotNull(addrMarry);
        
        Address addrSink = wallet.getAddress(LABEL_SINK);
        Assert.assertNotNull(wallet.getChangeAddress(LABEL_SINK));
        Assert.assertNotNull(addrSink);
        
        Address addrXXX = addrSink.setLabels(Arrays.asList("XXX"));
        Assert.assertEquals(Arrays.asList("XXX"), addrXXX.getLabels());
        Assert.assertEquals(addrSink.getAddress(), addrXXX.getAddress());
        
        addrSink = addrSink.setLabels(Arrays.asList(LABEL_SINK));
        Assert.assertEquals(Arrays.asList(LABEL_SINK), addrSink.getLabels());
        
        for (String label : labels) {
            List<Address> addrs = wallet.getAddresses(label);
            LOG.info(String.format("%-5s: addr=%s", label, addrs));
            Assert.assertTrue("At least one address", addrs.size() > 0);
        }
    }
    
    @Test
    public void testSimpleSpending () throws Exception {

        Blockchain blockchain = BlockchainFactory.getBlockchain();
        Network network = blockchain.getNetwork();
        Wallet wallet = blockchain.getWallet();
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        Assert.assertNotNull(wallet.getChangeAddress(LABEL_BOB));
        Assert.assertNotNull(addrBob);
        
        Address addrSink = wallet.getAddress(LABEL_SINK);
        Assert.assertNotNull(wallet.getChangeAddress(LABEL_SINK));
        Assert.assertNotNull(addrSink);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has no funds
        BigDecimal btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(BigDecimal.ZERO, btcBob);
        
        // Send 10 BTC to Bob
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("10.0"));
        
        // Mine the next block
        network.generate(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has received 10 BTC
        btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(10.0, btcBob.doubleValue(), 0);
        
        // Bob sends everything to the Sink  
        wallet.sendFromLabel(LABEL_BOB, addrSink.getAddress(), ALL_FUNDS);
        
        // Mine next block
        network.generate(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has no funds
        btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(BigDecimal.ZERO, btcBob);
    }
    
    @Test
    public void testNewAddress () throws Exception {
        
        Blockchain blockchain = BlockchainFactory.getBlockchain();
        Network network = blockchain.getNetwork();
        Wallet wallet = blockchain.getWallet();
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        Assert.assertNotNull(wallet.getChangeAddress(LABEL_BOB));
        Assert.assertNotNull(addrBob);
        
        Address addrSink = wallet.getAddress(LABEL_SINK);
        Assert.assertNotNull(wallet.getChangeAddress(LABEL_SINK));
        Assert.assertNotNull(addrSink);
        
        // Send 10 BTC to Bob
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("10.0"));
        
        // Mine the next block
        network.generate(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has received 10 BTC
        BigDecimal btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(10.0, btcBob.doubleValue(), 0);
        
        // Get a new address for Bob
        Address addrOther = wallet.newAddress(LABEL_BOB);
        Assert.assertFalse(addrBob.equals(addrOther));
        Assert.assertNotNull(addrOther.getPrivKey());
        
        // Send 4 BTC to the new address 
        BigDecimal btcSend = new BigDecimal("4.0");
        wallet.sendFromLabel(LABEL_BOB, addrOther.getAddress(), btcSend);
        
        // Mine the next block
        network.generate(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has received 10 BTC
        btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(subtractFee(new BigDecimal("10.0")).doubleValue(), btcBob.doubleValue(), 0);
        
        // Bob sends everything to the Sink  
        wallet.sendFromLabel(LABEL_BOB, addrSink.getAddress(), ALL_FUNDS);
        
        // Mine next block
        network.generate(1);
        
        // Verify that Bob has no funds
        btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(BigDecimal.ZERO, btcBob);
    }
}
