package io.nessus.test.bitcoin;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Tx;
import io.nessus.Tx.TxBuilder;
import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.bitcoin.AbstractBitcoinTest;

public class RawTxTest extends AbstractBitcoinTest {

    @Test
    public void testSimpleSpending () throws Exception {

    	// Send everything to the sink
    	wallet.sendFromAddress(addrBob, addrSink.getAddress(), Wallet.ALL_FUNDS);
    	wallet.sendFromAddress(addrMary, addrSink.getAddress(), Wallet.ALL_FUNDS);
    	
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has no funds
        BigDecimal btcBob = wallet.getBalance(addrBob);
        Assert.assertEquals(BigDecimal.ZERO, btcBob);
        
        // Send 10 BTC to Bob
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("10.0"));
        
        // Mine the next block
        network.generate(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has received 10 BTC
        btcBob = wallet.getBalance(addrBob);
        Assert.assertEquals(10.0, btcBob.doubleValue(), 0);
        
        // Select UTXOs that amount to >= 4.0 BTC
        BigDecimal btcSend = new BigDecimal("4.0");
        List<UTXO> utxos = wallet.selectUnspent(LABEL_BOB, btcSend);
        BigDecimal utxosAmount = getUTXOAmount(utxos);
        Assert.assertTrue("Cannot find sufficient funds", btcSend.compareTo(utxosAmount) <= 0);
        
        String changeAddr = wallet.getChangeAddress(LABEL_BOB).getAddress();
        BigDecimal changeAmount = utxosAmount.subtract(btcSend);
        changeAmount = changeAmount.subtract(network.getMinTxFee());
        
        Tx tx = new TxBuilder()
                .unspentInputs(utxos)
                .output(addrMary.getAddress(), btcSend)
                .output(changeAddr, changeAmount)
                .build();

        wallet.sendTx(tx);
        
        // Mine the next block
        network.generate(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Mary has received 4.0 BTC
        BigDecimal btcMary = wallet.getBalance(addrMary);
        Assert.assertEquals(btcSend.doubleValue(), btcMary.doubleValue(), 0);
        
        // Verify that Bob has spent 4.0 BTC
        btcBob = wallet.getBalance(addrBob);
        Assert.assertTrue(btcBob.compareTo(new BigDecimal("6.0")) <= 0);
    }
}
