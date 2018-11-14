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
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.bitcoin.AbstractBitcoinTest;

public class WalletTest extends AbstractBitcoinTest {

    Blockchain blockchain;
    Network network;
    Wallet wallet;
    
    @Before
    public void before() {
        
        blockchain = BlockchainFactory.getBlockchain();
        network = blockchain.getNetwork();
        wallet = blockchain.getWallet();
    }
    
    @After
    public void after() {
        
        // Bob & Mary send everything to the Sink  
        Address addrSink = wallet.getAddress(LABEL_SINK);
        wallet.sendFromLabel(LABEL_BOB, addrSink.getAddress(), ALL_FUNDS);
        wallet.sendFromLabel(LABEL_MARY, addrSink.getAddress(), ALL_FUNDS);
        network.generate(1);
    }
    
    @Test
    public void testInitialImport () throws Exception {

        List<String> labels = wallet.getLabels();
        Assert.assertEquals("[(change), Bob, Mary, Sink]", labels.toString());
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        Assert.assertNotNull(wallet.getChangeAddress(LABEL_BOB));
        Assert.assertNotNull(addrBob);
        
        Address addrMary = wallet.getAddress(LABEL_MARY);
        Assert.assertNotNull(wallet.getChangeAddress(LABEL_MARY));
        Assert.assertNotNull(addrMary);
        
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
    }
    
    @Test
    public void testNewAddress () throws Exception {
        
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
    }
    
    @Test
    public void testLockUnspent () throws Exception {
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        
        List<UTXO> utxos = wallet.listUnspent(Arrays.asList(addrBob));
        Assert.assertTrue("No utxos", utxos.isEmpty());
        
        // Send 10 BTC to Bob
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("10.0"));
        utxos = wallet.listUnspent(Arrays.asList(addrBob));
        Assert.assertEquals(1, utxos.size());
        
        UTXO utxo = utxos.get(0);
        Assert.assertEquals(addrBob.getAddress(), utxo.getAddress());
        Assert.assertEquals(new BigDecimal("10.0").doubleValue(), utxo.getAmount().doubleValue(), 0);

        List<UTXO> locked = wallet.listLockUnspent(Arrays.asList(addrBob));
        Assert.assertTrue("No utxos", locked.isEmpty());
        
        Assert.assertTrue(wallet.lockUnspent(utxo, false));
        
        utxos = wallet.listUnspent(Arrays.asList(addrBob));
        Assert.assertTrue("No utxos", utxos.isEmpty());
        
        Assert.assertTrue(wallet.lockUnspent(utxo, true));
        
        utxos = wallet.listUnspent(Arrays.asList(addrBob));
        Assert.assertEquals(1, utxos.size());
    }

    @Test
    public void testListUnspent() throws Exception {
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        
        List<UTXO> utxos = wallet.listUnspent(Arrays.asList(addrBob));
        Assert.assertTrue("No utxos", utxos.isEmpty());
        
        // Send 10 BTC to Bob
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("10.0"));
        utxos = wallet.listUnspent(Arrays.asList(addrBob));
        Assert.assertEquals(1, utxos.size());
        
        utxos = wallet.listUnspent(Collections.emptyList());
        utxos.forEach(utxo -> LOG.info("{}", utxo));
        Assert.assertTrue(utxos.isEmpty());
    }
}
