package io.nessus.test.bitcoin;

import static io.nessus.Wallet.ALL_FUNDS;
import static io.nessus.Wallet.LABEL_DEFAULT;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;

public class WalletTest extends AbstractRegtestTest {

    /**
     * Send 10 BTC to Bob from the default account 
     */
    @Test
    public void testSimpleSpending () throws Exception {

        Blockchain blockchain = BlockchainFactory.getBlockchain();
        Network network = blockchain.getNetwork();
        Wallet wallet = blockchain.getWallet();
        
        String addrBob = wallet.getAddress(LABEL_BOB).getAddress();
        String addrSink = wallet.getAddress(LABEL_SINK).getAddress();
        
        // Show account balances
        showAccountBalances();
        
        // Verify that the default account has some bitcoin
        BigDecimal btcMiner = wallet.getBalance(LABEL_DEFAULT);
        Assert.assertTrue(new BigDecimal("50.0").compareTo(btcMiner) <= 0);
        
        // Verify that Bob has no funds
        BigDecimal btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(BigDecimal.ZERO, btcBob);
        
        // Send 10 BTC to Bob
        wallet.sendToAddress(addrBob, new BigDecimal("10.0"));
        
        // Mine the next block
        network.generate(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has received 10 BTC
        btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(10.0, btcBob.doubleValue(), 0);
        
        // Bob sends everything to the Sink  
        wallet.sendFromLabel(LABEL_BOB, addrSink, ALL_FUNDS);
        
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
        
        String addrBob = wallet.getAddress(LABEL_BOB).getAddress();
        String addrSink = wallet.getAddress(LABEL_SINK).getAddress();
        
        // Send 10 BTC to Bob
        wallet.sendToAddress(addrBob, new BigDecimal("10.0"));
        
        // Mine the next block
        network.generate(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has received 10 BTC
        BigDecimal btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(10.0, btcBob.doubleValue(), 0);
        
        // Get a new address for Bob
        Address addrOther = wallet.getNewAddress(Arrays.asList(LABEL_BOB));
        Assert.assertFalse(addrBob.equals(addrOther.getAddress()));
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
        Assert.assertEquals(10.0 - estimateFee().doubleValue(), btcBob.doubleValue(), 0);
        
        // Bob sends everything to the Sink  
        wallet.sendFromLabel(LABEL_BOB, addrSink, ALL_FUNDS);
        
        // Mine next block
        network.generate(1);
        
        // Verify that Bob has no funds
        btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(BigDecimal.ZERO, btcBob);
    }
}
