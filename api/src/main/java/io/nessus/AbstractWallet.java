package io.nessus;

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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Tx.TxBuilder;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.BasicTxInput;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.BasicTxOutput;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.LockedUnspent;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.RawTransaction;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.RawTransaction.In;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.RawTransaction.Out;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.RawTransaction.Out.ScriptPubKey;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.Transaction;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.Unspent;
import wf.bitcoin.krotjson.HexCoder;

public abstract class AbstractWallet extends RpcClientSupport implements Wallet {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    private final Blockchain blockchain;

    protected AbstractWallet(Blockchain blockchain, BitcoindRpcClient client) {
        super(client);
        this.blockchain = blockchain;
    }

    protected Blockchain getBlockchain() {
        return blockchain;
    }

    @Override
    public void importAddresses(Config config) {
        AssertArgument.assertNotNull(config, "Null config");
        
        for (Config.Address addr : config.getWallet().getAddresses()) {
            String privKey = addr.getPrivKey();
            String pubKey = addr.getPubKey();
            try {
                if (privKey != null && pubKey == null) {
                    importPrivateKey(privKey, addr.getLabels());
                } else {
                    importAddress(pubKey, addr.getLabels());
                }
            } catch (BitcoinRPCException ex) {
                String message = ex.getMessage();
                if (!message.contains("walletpassphrase")) throw ex;
            }
        }
    }

    @Override
    public Address importPrivateKey(String privKey, List<String> labels) {
        AssertArgument.assertNotNull(privKey, "Null privKey");
        AssertArgument.assertNotNull(labels, "Null labels");

        // Check if we already have this privKey
        for (Address addr : getAddressMapping().values()) {
            if (privKey.equals(addr.getPrivKey())) {
                return addr;
            }
        }

        // Note, the bitcoin-core account system will be removed in 0.18.0
        String lstr = concatLabels(labels);
        LOG.info("Import privKey {} {}", privKey.substring(0, 2) + "************", lstr);

        boolean rescan = !blockchain.isPruned();
        client.importPrivKey(privKey, lstr, rescan);

        // bitcoin-core -regtest v0.16.0 generates three public addresses for
        // every private key that gets imported. These are
        // #1 P2PKH  e.g. n3ha6rJa8ZS7B4v4vwNWn8CnLHfUYXW1XE
        // #2 P2SH   e.g. 2Mxztt6zTZ7KPHMdPt8iYtSLxQ9FHAPJMns
        // #3 Bech32 e.g. bcrt1q7d2dzgxmgu4kzpmvrlz306kcffw225ydq2jwm7

        Address addr = null;
        for (Address auxaddr : getAddressMapping().values()) {
            if (privKey.equals(auxaddr.getPrivKey())) {
                addr = auxaddr;
                break;
            }
        }

        AssertState.assertNotNull(addr, "Cannot get imported address from wallet");
        return addr;
    }

    @Override
    public Address importAddress(String rawAddr, List<String> labels) {
        AssertArgument.assertNotNull(rawAddr, "Null privKey");
        AssertArgument.assertNotNull(labels, "Null labels");

        // Check if we already have this privKey
        for (Address addr : getAddressMapping().values()) {
            if (rawAddr.equals(addr.getAddress())) {
                return addr;
            }
        }

        // Note, the bitcoin-core account system will be removed in 0.18.0
        String lstr = concatLabels(labels);
        LOG.info("Import address {} {}", rawAddr, lstr);
        
        boolean rescan = !blockchain.isPruned();
        client.importAddress(rawAddr, lstr, rescan);

        return createAdddressFromRaw(rawAddr, labels);
    }

    @Override
    public final Address newAddress(String label) {
        AssertArgument.assertNotNull(label, "Null label");
        return createNewAddress(Arrays.asList(label));
    }

    @Override
    public final Address newChangeAddress(String label) {
        AssertArgument.assertNotNull(label, "Null label");
        return createNewAddress(Arrays.asList(label, LABEL_CHANGE));
    }

    @Override
    public List<String> getLabels() {
        Set<String> labels = new HashSet<>();
        getAddressMapping().values().stream().forEach(a -> labels.addAll(a.getLabels()));
        return labels.stream().sorted().collect(Collectors.toList());
    }

    @Override
    public Address getAddress(String label) {
        AssertArgument.assertNotNull(label, "Null label");

        List<Address> addrs = getAddresses(label);
        addrs = addrs.stream().filter(a -> !a.getLabels().contains(LABEL_CHANGE)).collect(Collectors.toList());
        return addrs != null && addrs.size() > 0 ? addrs.iterator().next() : null;
    }

    @Override
    public Address findAddress(String rawAddr) {
        AssertArgument.assertNotNull(rawAddr, "Null rawAddr");
        return getAddressMapping().values().stream().filter(a -> a.getAddress().equals(rawAddr)).findFirst().orElse(null);
    }

    @Override
    public List<Address> getAddresses() {
        return getAddressMapping().values().stream().collect(Collectors.toList());
    }

    @Override
    public List<Address> getAddresses(String label) {
        AssertArgument.assertNotNull(label, "Null label");
        
        List<Address> filtered = getAddressMapping().values().stream()
                .filter(a -> a.getLabels().contains(label))
                .collect(Collectors.toList());
        
        return filtered;
    }

    @Override
    public List<Address> getChangeAddresses(String label) {
        AssertArgument.assertNotNull(label, "Null label");
        
        List<Address> filtered = getAddresses(label).stream()
                .filter(a -> a.getLabels().contains(LABEL_CHANGE))
                .collect(Collectors.toList());
        
        return filtered;
    }

    @Override
    public Address getChangeAddress(String label) {
        AssertArgument.assertNotNull(label, "Null label");
        
        List<Address> addrs = getChangeAddresses(label);
        if (addrs.isEmpty()) {
            addrs.add(newChangeAddress(label));
        }
        if (addrs.size() == 1) {
            return addrs.get(0);
        }
        int idx = new Random().nextInt(addrs.size());
        return addrs.get(idx);
    }
    
    @Override
    public BigDecimal getBalance(String label) {
        List<Address> addrs = label != null ? getAddresses(label) : getAddresses();
        return getUTXOAmount(listUnspent(addrs));
    }

    @Override
    public BigDecimal getBalance(Address addr) {
        AssertArgument.assertNotNull(addr, "Null addr");
        return getUTXOAmount(listUnspent(Arrays.asList(addr)));
    }

    public static BigDecimal getUTXOAmount(List<UTXO> utxos) {
        AssertArgument.assertNotNull(utxos, "Null utxos");
        
        BigDecimal result = BigDecimal.ZERO;
        for (UTXO utxo : utxos) {
            result = result.add(utxo.getAmount());
        }
        return result;
    }
    
    @Override
    public String sendToAddress(String toAddr, BigDecimal amount) {
        AssertArgument.assertNotNull(toAddr, "Null toAddr");
        AssertArgument.assertNotNull(amount, "Null amount");
        
        return client.sendToAddress(toAddr, amount);
    }

    @Override
    public String sendFromLabel(String label, String toAddr, BigDecimal amount) {
        AssertArgument.assertNotNull(label, "Null label");
        AssertArgument.assertNotNull(toAddr, "Null toAddr");
        AssertArgument.assertNotNull(amount, "Null amount");

        List<UTXO> utxos = selectUnspent(label, amount);
        String changeAddr = getChangeAddress(label).getAddress();
        
        return sendToAddress(toAddr, changeAddr, amount, utxos);
    }

    @Override
    public String sendToAddress(String toAddr, String changeAddr, BigDecimal amount, List<UTXO> utxos) {
        AssertArgument.assertNotNull(toAddr, "Null toAddr");
        AssertArgument.assertNotNull(changeAddr, "Null changeAddr");
        AssertArgument.assertNotNull(amount, "Null amount");
        AssertArgument.assertNotNull(utxos, "Null utxos");

        // Nothing to do
        if (utxos.isEmpty()) return null;
        
        List<UTXO> utxoWithoutSPK = utxos.stream()
                .filter(utxo -> utxo.getScriptPubKey() == null)
                .collect(Collectors.toList());
        
        // Some APIs like listLockUnspent mey return a list of
        // UTXOs that have their scriptPubKey not initialised
        // https://github.com/jboss-fuse/nessus/issues/65
        
        if (!utxoWithoutSPK.isEmpty()) {
            
            List<Address> addrs = utxos.stream()
                    .map(utxo -> utxo.getAddress())
                    .map(raw -> findAddress(raw))
                    .collect(Collectors.toList());
            
            List<UTXO> futxos = utxos;
            List<UTXO> utxoWithSPK = listUnspent(addrs).stream()
                    .filter(utxo -> futxos.contains(utxo))
                    .collect(Collectors.toList());
            
            AssertState.assertEquals(utxos, utxoWithSPK);
            utxos = utxoWithSPK;
        }
        
        Network network = blockchain.getNetwork();
        BigDecimal dustAmount = network.getDustThreshold();
        BigDecimal utxosAmount = getUTXOAmount(utxos);
        BigDecimal sendAmount = utxosAmount.subtract(network.getMinTxFee());
        BigDecimal changeAmount = BigDecimal.ZERO;
        
        AssertState.assertTrue(sendAmount.doubleValue() <= utxosAmount.doubleValue(), "Cannot find sufficient funds");
        if (sendAmount.doubleValue() <= dustAmount.doubleValue()) {
            LOG.debug(String.format("UTXO Amount: %.6f", utxosAmount));
            LOG.debug(String.format("Send Amount: %.6f", sendAmount));
            LOG.warn("Cannot send less than dust amount: {}", sendAmount);
            return null;
        }
        
        Tx tmpTx = new TxBuilder()
                .unspentInputs(utxos)
                .output(toAddr, sendAmount)
                .build();
        
        // Estimate the fees based on Tx size
        byte[] bytes = HexCoder.decode(createRawTx(tmpTx));
        double kbytes = new Double(bytes.length) / 1024;
        BigDecimal feePerKB = network.estimateSmartFee(null);
        BigDecimal smartFee = new BigDecimal(String.format("%.6f", feePerKB.doubleValue() * kbytes));
        BigDecimal feeAmount = smartFee.max(network.getMinTxFee());
        
        LOG.debug(String.format("Smart Fee: %.6f", smartFee));
        LOG.debug(String.format("MinTx Fee: %.6f", network.getMinTxFee()));
        LOG.debug(String.format("Final Fee: %d bytes => %.6f", bytes.length, feeAmount));
        
        if (amount == ALL_FUNDS) {
            sendAmount = utxosAmount.subtract(feeAmount);
            changeAmount = BigDecimal.ZERO;
        } else {
            changeAmount = utxosAmount.subtract(sendAmount);
            changeAmount = changeAmount.subtract(feeAmount);
        }
        
        LOG.debug(String.format("UTXO Amount: %.6f", utxosAmount));
        LOG.debug(String.format("Send Amount: %.6f", sendAmount));
        LOG.debug(String.format("Change Amount: %.6f", changeAmount));
        
        TxBuilder builder = new TxBuilder()
                .unspentInputs(utxos)
                .output(toAddr, sendAmount);

        if (dustAmount.compareTo(changeAmount) < 0) {
            builder.output(changeAddr, changeAmount);
        }
        
        Tx tx = builder.build();
        
        String txId = sendTx(tx);

        LOG.debug("txId: {}", txId);

        return txId;
    }

    @Override
    public String sendTx(Tx tx) {
        AssertArgument.assertNotNull(tx, "Null tx");
        
        String rawTx = createRawTx(tx);
        String signedTx = signRawTx(rawTx, tx.inputs());
        return sendRawTransaction(signedTx);
    }

    public String createRawTx(Tx tx) {
        AssertArgument.assertNotNull(tx, "Null tx");
        
        List<BitcoindRpcClient.TxInput> auxIns = adaptInputs(tx.inputs());
        List<BitcoindRpcClient.TxOutput> auxOuts = adaptOutputs(tx.outputs());
        return client.createRawTransaction(auxIns, auxOuts);
    }

    public String signRawTx(String rawTx, List<TxInput> inputs) {
        AssertArgument.assertNotNull(rawTx, "Null rawTx");
        AssertArgument.assertNotNull(inputs, "Null inputs");
        
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
        AssertArgument.assertNotNull(signedTx, "Null signedTx");
        return client.sendRawTransaction(signedTx);
    }

    @Override
    public List<UTXO> listUnspent(String label) {
        AssertArgument.assertNotNull(label, "Null label");
        return listUnspent(getAddresses(label));
    }

    @Override
    public List<UTXO> listUnspent(List<Address> addrs) {
        AssertArgument.assertNotNull(addrs, "Null addrs");
        
        List<UTXO> result = new ArrayList<>();
        if (!addrs.isEmpty()) {
            List<String> rawAddrs = getRawAddresses(addrs);
            for (Unspent unspnt : client.listUnspent(0, Integer.MAX_VALUE, rawAddrs.toArray(new String[addrs.size()]))) {
                String txId = unspnt.txid();
                Integer vout = unspnt.vout();
                String addr = unspnt.address();
                String scriptPubKey = unspnt.scriptPubKey();
                BigDecimal amount = unspnt.amount();
                result.add(new UTXO(txId, vout, scriptPubKey, addr, amount));
            }
        }
        return result;
    }

    @Override
    public List<UTXO> listLockUnspent(List<Address> addrs) {
        AssertArgument.assertNotNull(addrs, "Null addrs");
        
        List<UTXO> result = new ArrayList<>();
        List<String> rawAddrs = getRawAddresses(addrs);
        for (LockedUnspent unspnt : client.listLockUnspent()) {
            
            String txId = unspnt.txId();
            Integer vout = unspnt.vout();
            Tx tx = getLockedTransaction(txId);
            if (tx != null) {
                TxOutput txout = tx.outputs().get(vout);
                String rawAddr = txout.getAddress();
                if (rawAddrs.contains(rawAddr)) {
                    BigDecimal amount = txout.getAmount();
                    result.add(new UTXO(txId, vout, null, rawAddr, amount));
                }
            }
        }
        return result;
    }

    // https://github.com/AegeusCoin/aegeus/issues/13
    protected Tx getLockedTransaction(String txId) {
        return getTransaction(txId);
    }
    
    @Override
    public boolean lockUnspent(UTXO utxo, boolean unlock) {
        AssertArgument.assertNotNull(utxo, "Null utxo");
        return client.lockUnspent(unlock, utxo.getTxId(), utxo.getVout());
    }

    @Override
    public List<UTXO> selectUnspent(String label, BigDecimal amount) {
        AssertArgument.assertNotNull(label, "Null label");
        AssertArgument.assertNotNull(amount, "Null amount");

        List<Address> addrs = getAddresses(label);
        return selectUnspent(addrs, amount);
    }

    @Override
    public List<UTXO> selectUnspent(List<Address> addrs, BigDecimal amount) {
        AssertArgument.assertNotNull(addrs, "Null addrs");
        AssertArgument.assertNotNull(amount, "Null amount");

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
        AssertArgument.assertNotNull(txId, "Null txId");
        
        Transaction transaction = client.getTransaction(txId);
        
        TxBuilder builder = new TxBuilder();
        builder.txId(transaction.txId());
        builder.blockHash(transaction.blockHash());
        builder.blockTime(transaction.blockTime());

        RawTransaction rawTx = transaction.raw();
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

        Tx txres = builder.build();
        return txres;
    }

    public Address updateAddress(Address addr, List<String> labels) {
        AssertArgument.assertNotNull(addr, "Null addr");
        AssertArgument.assertNotNull(labels, "Null labels");
        
        String rawAddr = addr.getAddress();
        String combined = concatLabels(labels);
        ((BitcoinJSONRPCClient) client).query("setaccount", rawAddr, combined);
        return findAddress(rawAddr);
    }
    
    public String redeemChange(String label, Address toAddr) {
        AssertArgument.assertNotNull(label, "Null label");
        AssertArgument.assertNotNull(toAddr, "Null toAddr");
        
        List<Address> addrs = getChangeAddresses(label);
        List<UTXO> utxos = listUnspent(addrs);
        
        return sendToAddress(toAddr.getAddress(), toAddr.getAddress(), Wallet.ALL_FUNDS, utxos);
    }
    
    protected abstract Address createAdddressFromRaw(String rawAddr, List<String> labels);

    protected abstract Address createNewAddress(List<String> labels);

    protected String concatLabels(List<String> labels) {
        String result = labels.toString();
        return result.substring(1, result.length() - 1);
    }
    
    protected List<String> splitLabels(String labels) {
        return Arrays.asList(labels.split(",")).stream().map(t -> t.trim()).collect(Collectors.toList());
    }
    
    private Map<String, Address> getAddressMapping() {
        Map<String, Address> result = new LinkedHashMap<>();
        for (String acc : client.listAccounts(0, true).keySet()) {
            for (String rawAddr : client.getAddressesByAccount(acc)) {
                if (isP2PKH(rawAddr)) {
                    result.put(rawAddr, createAdddressFromRaw(rawAddr, splitLabels(acc)));
                }
            }
        }
        
        return result;
    }
    
    private List<String> getRawAddresses(List<Address> addrs) {
        return addrs.stream().map(a -> a.getAddress()).collect(Collectors.toList());
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
