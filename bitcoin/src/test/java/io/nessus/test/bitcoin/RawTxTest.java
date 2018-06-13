package io.nessus.test.bitcoin;

import static io.nessus.Wallet.LABEL_DEFAULT;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.Tx;
import io.nessus.Tx.TxBuilder;
import io.nessus.UTXO;
import io.nessus.Wallet;

public class RawTxTest extends AbstractRegtestTest {

    /**
     * Send 10 BTC to Bob from the default account. 
     * Send 4 BTC to Marry keeping the change. 
     */
    @Test
    public void testSimpleSpending () throws Exception {

        Blockchain blockchain = BlockchainFactory.getBlockchain();
        Network network = blockchain.getNetwork();
        Wallet wallet = blockchain.getWallet();
        
        String addrBob = wallet.getAddress(LABEL_BOB).getAddress();
        String addrMarry = wallet.getAddress(LABEL_MARRY).getAddress();
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
        
        // Select UTOXs that amount to >= 4.0 BTC
        BigDecimal btcSend = new BigDecimal("4.0");
        List<UTXO> utxos = wallet.selectUnspent(LABEL_BOB, addFee(btcSend));
        Double utxosAmount = utxos.stream().mapToDouble(utxo -> utxo.getAmount().doubleValue()).sum();
        Assert.assertTrue("Cannot find sufficient funds", addFee(btcSend).doubleValue() <= utxosAmount);
        
        String changeAddr = wallet.getChangeAddress(LABEL_BOB).getAddress();
        BigDecimal changeAmount = new BigDecimal(utxosAmount - addFee(btcSend).doubleValue());
        
        Tx tx = new TxBuilder()
                .unspentInputs(utxos)
                .output(addrMarry, btcSend)
                .output(changeAddr, changeAmount)
                .build();

        wallet.sendTx(tx);
        
        // Mine the next block
        network.generate(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Marry has received 4.0 BTC
        BigDecimal btcMarry = wallet.getBalance(LABEL_MARRY);
        Assert.assertEquals(btcSend.doubleValue(), btcMarry.doubleValue(), 0);
        
        // Verify that Bob has spent 4.0 BTC
        btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertTrue(btcBob.compareTo(new BigDecimal("6.0")) <= 0);
        
        // Bob sends everything to the Sink  
        wallet.sendFromLabel(LABEL_BOB, addrSink, subtractFee(btcBob));
        
        // Marry sends everything to the Sink  
        wallet.sendFromLabel(LABEL_MARRY, addrSink, subtractFee(btcMarry));
        
        // Mine next block
        network.generate(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has no funds
        btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(BigDecimal.ZERO, btcBob);
        
        // Verify that Marry has no funds
        btcMarry = wallet.getBalance(LABEL_MARRY);
        Assert.assertEquals(BigDecimal.ZERO, btcMarry);
    }
}
