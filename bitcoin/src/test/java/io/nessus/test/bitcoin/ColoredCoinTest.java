package io.nessus.test.bitcoin;

import static io.nessus.Wallet.ALL_FUNDS;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.Tx;
import io.nessus.TxOutput;
import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.Tx.TxBuilder;

public class ColoredCoinTest extends AbstractRegtestTest {

    @Test
    public void testSimpleSpending () throws Exception {

        Blockchain blockchain = BlockchainFactory.getBlockchain();
        Network network = blockchain.getNetwork();
        Wallet wallet = blockchain.getWallet();
        
        String addrBob = wallet.getAddress(LABEL_BOB).getAddress();
        String addrMarry = wallet.getAddress(LABEL_MARRY).getAddress();
        String addrSink = wallet.getAddress(LABEL_SINK).getAddress();
        
        // Send 0.1 BTC to Bob
        wallet.sendToAddress(addrBob, new BigDecimal("0.1"));
        
        // Mine the next block
        network.generate(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has received 0.1 BTC
        BigDecimal btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(0.1, btcBob.doubleValue(), 0);
        
        BigDecimal btcSend = new BigDecimal("0.01");
        List<UTXO> utxos = wallet.selectUnspent(LABEL_BOB, addFee(btcSend));
        BigDecimal utxosAmount = getUTXOAmount(utxos);
        
        String changeAddr = wallet.getChangeAddress(LABEL_BOB).getAddress();
        BigDecimal changeAmount = utxosAmount.subtract(addFee(btcSend));
        
        byte[] data = "IPFS".getBytes();
        
        Tx tx = new TxBuilder()
                .unspentInputs(utxos)
                .output(new TxOutput(addrMarry, btcSend, data))
                .output(changeAddr, changeAmount)
                .build();
        
        String txId = wallet.sendTx(tx);
        
        // Mine the next block
        network.generate(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Marry has received 0.01 BTC
        BigDecimal btcMarry = wallet.getBalance(LABEL_MARRY);
        Assert.assertEquals(btcSend.doubleValue(), btcMarry.doubleValue(), 0);
        
        // Verify that OP_RETURN data has been recorded
        tx = wallet.getTransaction(txId);
        TxOutput dataOut = tx.getOutputs().stream().filter(o -> o.getData() != null).findFirst().get();
        Assert.assertArrayEquals(data, dataOut.getData());
        
        // Bob sends everything to the Sink  
        wallet.sendFromLabel(LABEL_BOB, addrSink, ALL_FUNDS);
        
        // Marry sends everything to the Sink  
        wallet.sendFromLabel(LABEL_MARRY, addrSink, ALL_FUNDS);
        
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
