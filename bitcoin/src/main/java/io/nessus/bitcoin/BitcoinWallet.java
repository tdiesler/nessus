package io.nessus.bitcoin;

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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.Tx;
import io.nessus.Tx.TxBuilder;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;
import io.nessus.TxInput;
import io.nessus.TxOutput;
import io.nessus.UTXO;
import io.nessus.Wallet;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.BasicTxInput;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.BasicTxOutput;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.RawTransaction;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.RawTransaction.In;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.RawTransaction.Out;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.RawTransaction.Out.ScriptPubKey;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.Transaction;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.Unspent;
import wf.bitcoin.krotjson.HexCoder;

public class BitcoinWallet extends BitcoinClientSupport implements Wallet {

    static final Logger LOG = LoggerFactory.getLogger(BitcoinWallet.class);

    private final Set<Address> addressses = new LinkedHashSet<>();

    private final Blockchain blockchain;

    protected BitcoinWallet(BitcoinBlockchain blockchain, BitcoindRpcClient client) {
        super(client);
        
        this.blockchain = blockchain;

        for (String addr : client.getAddressesByAccount("")) {
            if (isP2PKH(addr)) {
                BitcoinAddress aux = new BitcoinAddress(this, addr, Collections.emptyList());
                addressses.add(aux);
            }
        }
    }

    @Override
    public Address addPrivateKey(String privKey, List<String> labels) {

        // Check if we already have this privKey
        for (Address addr : addressses) {
            if (privKey.equals(addr.getPrivKey())) {
                addr.addLabels(labels);
                return addr;
            }
        }
        
        // Note, this does not use the bitcoin-core account system 
        // as this will be removed in 0.18.0
        client.importPrivKey(privKey, "", true);

        // bitcoin-core -regtest v0.16.0 generates three public addresses for
        // every private key that gets imported. These are
        // #1 P2PKH  e.g. n3ha6rJa8ZS7B4v4vwNWn8CnLHfUYXW1XE
        // #2 P2SH   e.g. 2Mxztt6zTZ7KPHMdPt8iYtSLxQ9FHAPJMns
        // #3 Bech32 e.g. bcrt1q7d2dzgxmgu4kzpmvrlz306kcffw225ydq2jwm7

        String address = null;
        for (String auxAddr : client.getAddressesByAccount("")) {
            String auxKey = client.dumpPrivKey(auxAddr);

            // This associates the addresses that are initially derived
            // from theis private key with the account
            if (privKey.equals(auxKey)) {
                address = auxAddr;
                break;
            }
        }

        BitcoinAddress addr = new BitcoinAddress(this, address, labels);
        addressses.add(addr);

        return addr;
    }

    @Override
    public Address addAddress(String address, List<String> labels) {

        // Note, this does not use the bitcoin-core account system 
        // as this will be removed in 0.18.0
        client.importAddress(address, "", true);

        BitcoinAddress addr = new BitcoinAddress(this, address, labels);
        addressses.add(addr);

        return addr;
    }

    @Override
    public final Address newAddress(List<String> labels) {
        BitcoinAddress addr = createNewAddress(labels);
        addressses.add(addr);
        return addr;
    }

    protected BitcoinAddress createNewAddress(List<String> labels) {
        String auxAddr = assertP2PKH(client.getNewAddress("", "legacy"));
        return new BitcoinAddress(this, auxAddr, labels);
    }

    @Override
    public List<String> getLabels() {
        Set<String> labels = new HashSet<>();
        addressses.stream().forEach(a -> labels.addAll(a.getLabels()));
        return labels.stream().sorted().collect(Collectors.toList());
    }

    @Override
    public Address getAddress(String label) {
        List<Address> addrs = getAddresses(label);
        return addrs != null && addrs.size() > 0 ? addrs.iterator().next() : null;
    }

    @Override
    public Address findAddress(String rawAddr) {
        return addressses.stream().filter(a -> a.getAddress().equals(rawAddr)).findFirst().orElse(null);
    }

    @Override
    public List<Address> getAddresses() {
        return addressses.stream().collect(Collectors.toList());
    }

    @Override
    public List<Address> getAddresses(String label) {
        AssertArgument.assertNotNull(label, "label");
        List<Address> filtered = addressses.stream()
                .filter(a -> a.getLabels().contains(label))
                .collect(Collectors.toList());
        return filtered;
    }

    @Override
    public Address getChangeAddress(String label) {
        List<Address> addrs = getChangeAddresses(label);
        return addrs != null && addrs.size() > 0 ? addrs.iterator().next() : null;
    }

    @Override
    public List<Address> getChangeAddresses(String label) {
        List<Address> filtered = addressses.stream()
                .filter(a -> a.getLabels().contains(label))
                .filter(a -> a.getLabels().contains(LABEL_CHANGE))
                .collect(Collectors.toList());
        return filtered;
    }

    @Override
    public BigDecimal getBalance(String label) {
        return getUTXOAmount(listUnspent(getAddresses(label)));
    }

    @Override
    public BigDecimal getBalance(Address addr) {
        return getUTXOAmount(listUnspent(Arrays.asList(addr)));
    }

    @Override
    public String sendToAddress(String toAddress, BigDecimal amount) {
        return client.sendToAddress(toAddress, amount);
    }

    @Override
    public String sendFromLabel(String label, String toAddress, BigDecimal amount) {

        BigDecimal estFee = blockchain.getNetwork().estimateFee();
        
        Tx tx;
        if (amount != ALL_FUNDS) {
            
            BigDecimal amountPlusFee = amount.add(estFee);

            List<UTXO> utxos = selectUnspent(label, amountPlusFee);
            BigDecimal utxosAmount = getUTXOAmount(utxos);
            AssertState.assertTrue(amountPlusFee.doubleValue() <= utxosAmount.doubleValue(), "Cannot find sufficient funds");

            String changeAddr = getChangeAddress(label).getAddress();
            BigDecimal changeAmount = utxosAmount.subtract(amountPlusFee);

            TxBuilder builder = new TxBuilder().unspentInputs(utxos).output(toAddress, amount);

            if (0 < changeAmount.doubleValue())
                builder.output(changeAddr, changeAmount);

            tx = builder.build();
            
        } else {
            
            List<UTXO> utxos = listUnspent(label);
            BigDecimal utxosAmount = getUTXOAmount(utxos);
            BigDecimal sendAmount = utxosAmount.subtract(estFee);
            
            TxBuilder builder = new TxBuilder().unspentInputs(utxos).output(toAddress, sendAmount);
            tx = builder.build();
        }

        String txId = sendTx(tx);
        LOG.debug("txId: {}", txId);

        return txId;
    }

    @Override
    public String sendTx(Tx tx) {
        String rawTx = createRawTx(tx);
        String signedTx = signRawTx(rawTx, tx.inputs());
        return sendRawTransaction(signedTx);
    }

    public String createRawTx(Tx tx) {
        return client.createRawTransaction(adaptInputs(tx.inputs()), adaptOutputs(tx.outputs()));
    }

    public String signRawTx(String rawTx, List<TxInput> inputs) {
        List<String> privKeys = new ArrayList<>();
        for (TxInput txin : inputs) {
            UTXO utxo = (UTXO) txin;
            Address addr = findAddress(utxo.getAddress());
            String privKey = addr.getPrivKey();
            privKeys.add(privKey);
        }
        return client.signRawTransaction(rawTx, adaptInputs(inputs), privKeys);
    }

    public String sendRawTransaction(String signedTx) {
        return client.sendRawTransaction(signedTx);
    }

    @Override
    public List<UTXO> listUnspent(String label) {
        return listUnspent(getAddresses(label));
    }

    @Override
    public List<UTXO> listUnspent(List<Address> addrs) {
        List<UTXO> result = new ArrayList<>();
        List<String> rawAddrs = getRawAddresses(addrs);
        for (Unspent unspnt : client.listUnspent(0, Integer.MAX_VALUE, rawAddrs.toArray(new String[addrs.size()]))) {
            String txId = unspnt.txid();
            Integer vout = unspnt.vout();
            String addr = unspnt.address();
            String scriptPubKey = unspnt.scriptPubKey();
            BigDecimal amount = unspnt.amount();
            result.add(new UTXO(txId, vout, scriptPubKey, addr, amount));
        }
        return result;
    }

    @Override
    public List<UTXO> selectUnspent(String label, BigDecimal amount) {

        List<Address> addrs = getAddresses(label);
        return selectUnspent(addrs, amount);
    }

    @Override
    public List<UTXO> selectUnspent(List<Address> addrs, BigDecimal amount) {

        BigDecimal total = BigDecimal.ZERO;
        List<UTXO> result = new ArrayList<>();

        // Naively use the utxo up th the requested amount
        // in the order given by the underlying wallet
        for (UTXO utxo : listUnspent(addrs)) {
            result.add(utxo);
            total = total.add(utxo.getAmount());
            if (amount != ALL_FUNDS && amount.compareTo(total) <= 0)
                break;
        }

        AssertState.assertTrue(amount.compareTo(total) <= 0, "Insufficient funds: " + total);
        
        return result;
    }

    @Override
    public Tx getTransaction(String txId) {
        
        Transaction tx = client.getTransaction(txId);
        
        TxBuilder builder = new TxBuilder();
        builder.txId(tx.txId());
        builder.blockHash(tx.blockHash());
        builder.blockTime(tx.blockTime());

        RawTransaction rawTx = tx.raw();
        if (rawTx != null) {
            for (In in : rawTx.vIn()) {
                TxInput txIn = new TxInput(in.txid(), in.vout(), in.scriptPubKey());
                builder.input(txIn);
            }
            for (Out out : rawTx.vOut()) {
                ScriptPubKey spk = out.scriptPubKey();
                List<String> addrs = spk.addresses();
                String addr = null;
                if (addrs != null && addrs.size() > 0) {
                    if (addrs.size() > 1) LOG.warn("Multiple addresses not supported");
                    addr = addrs.get(0);
                }
                
                byte[] data = null;
                String hex = spk.hex();
                String type = spk.type();
                
                // OP_RETURN
                byte op = HexCoder.decode(hex.substring(0, 2))[0];
                if (op == 0x6A) {
                    data = HexCoder.decode(hex);
                }
                
                TxOutput txOut = new TxOutput(addr, out.value(), data);
                txOut.setType(type);
                
                builder.output(txOut);
            }
        }

        return builder.build();
    }

    protected boolean isP2PKH(String addr) {
        // https://en.bitcoin.it/wiki/List_of_address_prefixes
        return addr.startsWith("1") || addr.startsWith("m") || addr.startsWith("n");
    }

    protected String assertP2PKH(String addr) {
        AssertState.assertTrue(isP2PKH(addr), "Not a P2PKH address: " + addr);
        return addr;
    }

    private List<String> getRawAddresses(List<Address> addrs) {
        return addrs.stream().map(a -> a.getAddress()).collect(Collectors.toList());
    }

    private BigDecimal getUTXOAmount(List<UTXO> utxos) {
        BigDecimal result = BigDecimal.ZERO;
        for (UTXO utxo : utxos) {
            result = result.add(utxo.getAmount());
        }
        return result;
    }
    
    private List<BitcoindRpcClient.TxInput> adaptInputs(List<TxInput> inputs) {
        List<BitcoindRpcClient.TxInput> result = new ArrayList<>();
        for (TxInput aux : inputs) {
            result.add(new BasicTxInput(aux.getTxId(), aux.getVout(), aux.getScriptPubKey()));
        }
        return result;
    }

    private List<BitcoindRpcClient.TxOutput> adaptOutputs(List<TxOutput> outputs) {
        List<BitcoindRpcClient.TxOutput> result = new ArrayList<>();
        for (TxOutput aux : outputs) {
            result.add(new BasicTxOutput(aux.getAddress(), aux.getAmount(), aux.getData()));
        }
        return result;
    }
}
