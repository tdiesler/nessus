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
import io.nessus.Wallet.Address;
import io.nessus.bitcoin.AbstractBitcoinTest;

public class RawTxTest extends AbstractBitcoinTest {

    @Test
    public void testSimpleSpending () throws Exception {

        Blockchain blockchain = BlockchainFactory.getBlockchain();
        Network network = blockchain.getNetwork();
        Wallet wallet = blockchain.getWallet();
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        Address addrMarry = wallet.getAddress(LABEL_MARRY);
        Address addrSink = wallet.getAddress(LABEL_SINK);
        
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
        
        // Select UTXOs that amount to >= 4.0 BTC
        BigDecimal btcSend = new BigDecimal("4.0");
        List<UTXO> utxos = wallet.selectUnspent(LABEL_BOB, addFee(btcSend));
        BigDecimal utxosAmount = getUTXOAmount(utxos);
        Assert.assertTrue("Cannot find sufficient funds", addFee(btcSend).compareTo(utxosAmount) <= 0);
        
        String changeAddr = wallet.getChangeAddress(LABEL_BOB).getAddress();
        BigDecimal changeAmount = utxosAmount.subtract(addFee(btcSend));
        
        Tx tx = new TxBuilder()
                .unspentInputs(utxos)
                .output(addrMarry.getAddress(), btcSend)
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
        wallet.sendFromLabel(LABEL_BOB, addrSink.getAddress(), ALL_FUNDS);
        
        // Marry sends everything to the Sink  
        wallet.sendFromLabel(LABEL_MARRY, addrSink.getAddress(), ALL_FUNDS);
        
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
