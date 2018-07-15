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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.nessus.AssertArgument;
import io.nessus.AssertState;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.Tx;
import io.nessus.Tx.TxBuilder;
import io.nessus.TxInput;
import io.nessus.TxOutput;
import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.bitcoin.AbstractBitcoinTest;
import io.nessus.bitcoin.BitcoinBlockchain;
import io.nessus.bitcoin.BitcoinWallet;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;
import wf.bitcoin.krotjson.HexCoder;

public class ColoredCoinTest extends AbstractBitcoinTest {

    BitcoinBlockchain blockchain;
    Address addrBob;
    Address addrMarry;
    Address addrSink;
    
    @Before
    public void before() {
        
        blockchain = (BitcoinBlockchain) BlockchainFactory.getBlockchain();
        Network network = blockchain.getNetwork();
        Wallet wallet = blockchain.getWallet();
        
        addrBob = wallet.getAddress(LABEL_BOB);
        addrMarry = wallet.getAddress(LABEL_MARRY);
        addrSink = wallet.getAddress(LABEL_SINK);
        
        // Send 0.1 BTC to Bob
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("0.1"));
        network.generate(1);
    }
    
    @After
    public void after() {
        
        Wallet wallet = blockchain.getWallet();
        
        // Bob & Marry send everything to the Sink  
        wallet.sendFromLabel(LABEL_BOB, addrSink.getAddress(), ALL_FUNDS);
        wallet.sendFromLabel(LABEL_MARRY, addrSink.getAddress(), ALL_FUNDS);
    }
    
    @Test
    public void testSimpleSpending () throws Exception {

        Wallet wallet = blockchain.getWallet();
        Network network = blockchain.getNetwork();
        
        // Verify that Bob has received 0.1 BTC
        BigDecimal btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(0.1, btcBob.doubleValue(), 0);
        
        BigDecimal btcSend = new BigDecimal("0.01");
        List<UTXO> utxos = wallet.selectUnspent(LABEL_BOB, addFee(btcSend));
        BigDecimal utxosAmount = getUTXOAmount(utxos);
        
        String changeAddr = wallet.getChangeAddress(LABEL_BOB).getAddress();
        BigDecimal changeAmount = utxosAmount.subtract(addFee(btcSend));
        
        byte[] dataIn = "IPFS".getBytes();
        
        Tx tx = new TxBuilder()
                .unspentInputs(utxos)
                .output(changeAddr, changeAmount)
                .output(new TxOutput(addrMarry.getAddress(), btcSend, dataIn))
                .build();
        
        String txId = wallet.sendTx(tx);
        LOG.info("Tx: {}", txId);
        
        // Mine the next block
        network.generate(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Marry has received 0.01 BTC
        BigDecimal btcMarry = wallet.getBalance(LABEL_MARRY);
        Assert.assertEquals(btcSend.doubleValue(), btcMarry.doubleValue(), 0);
        
        // Verify that OP_RETURN data has been recorded
        tx = wallet.getTransaction(txId);
        List<TxOutput> outputs = tx.outputs();
        Assert.assertEquals(3, outputs.size());
        Assert.assertEquals(changeAddr, outputs.get(0).getAddress());
        Assert.assertEquals(addrMarry.getAddress(), outputs.get(1).getAddress());
        Assert.assertNotNull(outputs.get(2).getData());
        byte[] dataOut = outputs.get(2).getData();
        Assert.assertEquals("Expected OP_RETURN", 0x6A, dataOut[0]);
        Assert.assertEquals(dataOut.length, dataIn.length + 2);
        Assert.assertArrayEquals(dataIn, Arrays.copyOfRange(dataOut, 2, dataOut.length));
    }

    
    @Test
    public void testRawTxHack () throws Exception {

        Wallet wallet = blockchain.getWallet();
        Network network = blockchain.getNetwork();
        
        // Verify that Bob has received 0.1 BTC
        BigDecimal btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(0.1, btcBob.doubleValue(), 0);
        
        BigDecimal btcSend = new BigDecimal("0.01");
        List<UTXO> utxos = wallet.selectUnspent(LABEL_BOB, addFee(btcSend));
        BigDecimal utxosAmount = getUTXOAmount(utxos);
        
        String changeAddr = wallet.getChangeAddress(LABEL_BOB).getAddress();
        BigDecimal changeAmount = utxosAmount.subtract(addFee(btcSend));
        
        byte[] dataIn = "IPFS".getBytes();
        
        Tx tx = new TxBuilder()
                .unspentInputs(utxos)
                .output(changeAddr, changeAmount)
                .output(new TxOutput(addrMarry.getAddress(), btcSend))
                .build();
        
        ExtWallet extWallet = new ExtWallet(blockchain, blockchain.getRpcClient());
        String rawTx = extWallet.createRawTx(tx, dataIn);
        LOG.info("Raw Tx: {}", rawTx);
        
        String signedTx = extWallet.signRawTx(rawTx, tx.inputs());
        String txId = extWallet.sendRawTransaction(signedTx);
        
        LOG.info("Tx: {}", txId);
        
        // Mine the next block
        network.generate(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Marry has received 0.01 BTC
        BigDecimal btcMarry = wallet.getBalance(LABEL_MARRY);
        Assert.assertEquals(btcSend.doubleValue(), btcMarry.doubleValue(), 0);
        
        // Verify that OP_RETURN data has been recorded
        tx = wallet.getTransaction(txId);
        List<TxOutput> outputs = tx.outputs();
        Assert.assertEquals(3, outputs.size());
        Assert.assertEquals(changeAddr, outputs.get(0).getAddress());
        Assert.assertEquals(addrMarry.getAddress(), outputs.get(1).getAddress());
        Assert.assertNotNull(outputs.get(2).getData());
        byte[] dataOut = outputs.get(2).getData();
        Assert.assertEquals("Expected OP_RETURN", 0x6A, dataOut[0]);
        Assert.assertEquals(dataOut.length, dataIn.length + 2);
        Assert.assertArrayEquals(dataIn, Arrays.copyOfRange(dataOut, 2, dataOut.length));
    }

    class ExtWallet extends BitcoinWallet {

        protected ExtWallet(BitcoinBlockchain blockchain, BitcoindRpcClient client) {
            super(blockchain, client);
        }
        
        public String createRawTx(Tx tx, byte[] dataIn) {
            
            if (dataIn == null) {
                String rawTx = super.createRawTx(tx);
                return rawTx;
            }
            
            AssertArgument.assertTrue(dataIn.length <= 80, "Cannot encode more tahn 80 bytes of data");
            
            String suffix = "00000000";
            String dummyAddr = "2MwgFAFb8m7W7Dko1mM8vFQtNMngHWW5Mui";
            
            tx = new TxBuilder()
                    .inputs(tx.inputs())
                    .outputs(tx.outputs())
                    .output(new TxOutput(dummyAddr, BigDecimal.ZERO))
                    .build();
            
            String rawTx = super.createRawTx(tx);
            
            int zeroAmountIdx = rawTx.lastIndexOf("0000000000000000");
            AssertState.assertNotNull(zeroAmountIdx, "Cannot find zero amount index: " + rawTx);
            AssertState.assertTrue(rawTx.endsWith(suffix), "Unsupported final bytes: " + rawTx);

            byte[] scriptData = new byte[dataIn.length + 3];
            scriptData[0] = (byte) (dataIn.length + 2);
            scriptData[1] = 0x6a; // OP_RETURN
            scriptData[2] = (byte) dataIn.length;
            System.arraycopy(dataIn, 0, scriptData, 3, dataIn.length);
            
            rawTx = rawTx.substring(0, zeroAmountIdx + 16);
            rawTx = rawTx + HexCoder.encode(scriptData);
            rawTx = rawTx + suffix;
            
            return rawTx;
        }

        @Override
        public String signRawTx(String rawTx, List<TxInput> inputs) {
            return super.signRawTx(rawTx, inputs);
        }

        @Override
        public String sendRawTransaction(String signedTx) {
            return super.sendRawTransaction(signedTx);
        }
    }
}
