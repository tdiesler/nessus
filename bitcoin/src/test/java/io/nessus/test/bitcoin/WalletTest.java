package io.nessus.test.bitcoin;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Network;
import io.nessus.Wallet;

public class WalletTest extends AbstractRegtestTest {

    /**
     * Send 10 BTC to Bob from the default account 
     */
    @Test
    public void testSimpleSpending () throws Exception {

        Network network = getNetwork();
        Wallet wallet = getWallet();
        
        String addrBob = wallet.getDefaultAddress(LABEL_BOB).getAddress();
        String addrSink = wallet.getDefaultAddress(LABEL_SINK).getAddress();
        
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
        network.mineBlocks(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has received 10 BTC
        btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(10.0, btcBob.doubleValue(), 0);
        
        // Bob sends everything to the Sink  
        wallet.sendFromLabel(LABEL_BOB, addrSink, subtractFee(btcBob));
        
        // Mine next block
        network.mineBlocks(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has no funds
        btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(BigDecimal.ZERO, btcBob);
    }
}
