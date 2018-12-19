package io.nessus.test.bitcoin;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.bitcoin.AbstractBitcoinTest;

public class WalletTest extends AbstractBitcoinTest {

    @Test
    public void testInitialImport () throws Exception {

        List<String> labels = wallet.getLabels();
        Arrays.asList("(change)", "Bob", "Mary").forEach(label -> {
            Assert.assertTrue(labels + " contains: " + label, labels.contains(label));
        });
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        Assert.assertNotNull(wallet.getChangeAddress(LABEL_BOB));
        Assert.assertNotNull(addrBob);
        
        Address addrMary = wallet.getAddress(LABEL_MARY);
        Assert.assertNotNull(wallet.getChangeAddress(LABEL_MARY));
        Assert.assertNotNull(addrMary);
        
        Address addrSink = wallet.getAddress(LABEL_SINK);
        Assert.assertNotNull(addrSink);
        
        Address addrXXX = addrSink.setLabels(Arrays.asList("XXX"));
        Assert.assertEquals(Arrays.asList("XXX"), addrXXX.getLabels());
        Assert.assertEquals(addrSink.getAddress(), addrXXX.getAddress());
        
        addrSink = addrSink.setLabels(Arrays.asList(LABEL_SINK));
        Assert.assertEquals(Arrays.asList(LABEL_SINK), addrSink.getLabels());
        
        for (String label : labels) {
            List<Address> addrs = wallet.getAddresses(label);
            LOG.debug(String.format("%-5s: addr=%s", label, addrs));
            Assert.assertTrue("At least one address", addrs.size() > 0);
        }
    }
    
    @Test
    public void testSimpleSpending () throws Exception {

        // Show account balances
        showAccountBalances();
        
        // Send 10 BTC to Bob
        BigDecimal amount = new BigDecimal("10.0");
		wallet.sendToAddress(addrBob.getAddress(), amount);
        
        // Mine the next block
        network.generate(1, addrSink);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has received 10 BTC
        BigDecimal btcBob = wallet.getBalance(addrBob);
        Assert.assertTrue(amount.compareTo(btcBob) <= 0);
    }
    
    @Test
    public void testNewAddress () throws Exception {
        
        // Send everything to the sink
    	wallet.sendFromAddress(addrBob, addrSink.getAddress(), Wallet.ALL_FUNDS);
        
        // Verify that Bob has nothing
        BigDecimal btcBob = wallet.getBalance(addrBob);
        Assert.assertEquals(0.0, btcBob.doubleValue(), 0);
        
        // Send 10 BTC to Bob
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("10.0"));
        
        // Mine the next block
        network.generate(1, addrSink);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has received 10 BTC
        btcBob = wallet.getBalance(addrBob);
        Assert.assertEquals(10.0, btcBob.doubleValue(), 0);
        
        // Get a new address for Bob
        Address addrOther = wallet.newAddress(LABEL_BOB);
        Assert.assertFalse(addrBob.equals(addrOther));
        Assert.assertNotNull(addrOther.getPrivKey());
        
        // Send 4 BTC to the new address 
        BigDecimal btcSend = new BigDecimal("4.0");
        wallet.sendFromLabel(LABEL_BOB, addrOther.getAddress(), btcSend);
        
        // Mine the next block
        network.generate(1, addrSink);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has nearly 10 BTC
        btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertTrue(new BigDecimal("9.9999").doubleValue() < btcBob.doubleValue());
    }
    
    @Test
    public void testLockUnspent () throws Exception {
        
        // Send everything to the sink
    	wallet.sendFromAddress(addrBob, addrSink.getAddress(), Wallet.ALL_FUNDS);
        
        // Verify that Bob has no unspent funds
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
