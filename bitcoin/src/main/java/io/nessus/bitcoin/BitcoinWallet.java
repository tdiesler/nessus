package io.nessus.bitcoin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.AssertArgument;
import io.nessus.AssertState;
import io.nessus.Blockchain;
import io.nessus.Tx;
import io.nessus.Tx.TxBuilder;
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

public class BitcoinWallet implements Wallet {

    static final Logger LOG = LoggerFactory.getLogger(BitcoinWallet.class);

    private final Set<Address> addressses = new LinkedHashSet<>();

    private final Blockchain blockchain;
    private final BitcoindRpcClient client;

    BitcoinWallet(Blockchain blockchain, BitcoindRpcClient client) {
        this.blockchain = blockchain;
        this.client = client;

        for (String addr : client.getAddressesByAccount("")) {
            if (isP2PKH(addr)) {
                String privKey = client.dumpPrivKey(addr);
                DefaultAddress aux = new DefaultAddress(addr, privKey != null, Collections.emptyList());
                LOG.info(String.format("%s", aux));
                addressses.add(aux);
            }
        }
    }

    @Override
    public Address addPrivateKey(String privKey, List<String> labels) {

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
            LOG.debug("{} => {}", auxKey, auxAddr);

            // This associates the addresses that are initially derived
            // from theis private key with the account
            if (privKey.equals(auxKey)) {
                address = auxAddr;
                break;
            }
        }

        DefaultAddress addr = new DefaultAddress(address, false, labels);
        addressses.add(addr);

        return addr;
    }

    @Override
    public Address addAddress(String address, List<String> labels) {

        // Note, this does not use the bitcoin-core account system 
        // as this will be removed in 0.18.0
        client.importAddress(address, "", true);

        DefaultAddress addr = new DefaultAddress(address, true, labels);
        addressses.add(addr);

        return addr;
    }

    @Override
    public Address getNewAddress(List<String> labels) {
        String auxAddr = assertP2PKH(client.getNewAddress("", "legacy"));
        DefaultAddress addr = new DefaultAddress(auxAddr, false, labels);
        addressses.add(addr);
        return addr;
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
    public List<Address> getAddresses(String label) {
        List<Address> filtered = addressses.stream().filter(a -> label == null || a.getLabels().contains(label)).collect(Collectors.toList());
        return filtered;
    }

    @Override
    public List<String> getRawAddresses(String label) {
        return getAddresses(label).stream().map(a -> a.getAddress()).collect(Collectors.toList());
    }

    @Override
    public Address getChangeAddress(String label) {
        List<Address> addrs = getChangeAddresses(label);
        return addrs != null && addrs.size() > 0 ? addrs.iterator().next() : null;
    }

    @Override
    public List<Address> getChangeAddresses(String label) {
        List<Address> filtered = addressses.stream().filter(a -> a.getLabels().contains(label)).filter(a -> a.getLabels().contains(LABEL_CHANGE))
                .collect(Collectors.toList());
        return filtered;
    }

    @Override
    public List<String> getRawChangeAddresses(String label) {
        return getChangeAddresses(label).stream().map(a -> a.getAddress()).collect(Collectors.toList());
    }

    @Override
    public BigDecimal getBalance(String label) {
        List<String> addrs = getRawAddresses(label);
        return getUTXOAmount(listUnspent(addrs));
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
        String signedTx = signTx(tx);
        return client.sendRawTransaction(signedTx);
    }

    @Override
    public List<UTXO> listUnspent(String label) {
        List<String> addrs = getRawAddresses(label);
        return listUnspent(addrs);
    }

    @Override
    public List<UTXO> listUnspent(List<String> addrs) {
        List<UTXO> result = new ArrayList<>();
        for (Unspent unspnt : client.listUnspent(0, Integer.MAX_VALUE, addrs.toArray(new String[addrs.size()]))) {
            String txId = unspnt.txid();
            int vout = unspnt.vout();
            String addr = unspnt.address();
            String scriptPubKey = unspnt.scriptPubKey();
            BigDecimal amount = unspnt.amount();
            result.add(new UTXO(txId, vout, scriptPubKey, addr, amount));
        }
        return result;
    }

    
    @Override
    public List<UTXO> selectUnspent(String label, BigDecimal amount) {

        BigDecimal total = BigDecimal.ZERO;
        List<UTXO> result = new ArrayList<>();

        // Naively use the utxo up th the requested amount
        // in the order given by the underlying wallet
        for (UTXO utxo : listUnspent(label)) {
            result.add(utxo);
            total = total.add(utxo.getAmount());
            if (amount != ALL_FUNDS && amount.compareTo(total) <= 0)
                break;
        }

        return result;
    }

    @Override
    public Tx getTransaction(String txId) {
        Transaction tx = client.getTransaction(txId);
        RawTransaction rawTx = tx.raw();
        TxBuilder builder = new TxBuilder();
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
                data = HexCoder.decode(hex.substring(4));
            }
            
            TxOutput txOut = new TxOutput(addr, out.value(), data);
            txOut.setType(type);
            
            builder.output(txOut);
        }
        return builder.build();
    }

    private boolean isP2PKH(String addr) {
        // https://en.bitcoin.it/wiki/List_of_address_prefixes
        return addr.startsWith("1") || addr.startsWith("m") || addr.startsWith("n");
    }

    private String assertP2PKH(String addr) {
        AssertState.assertTrue(isP2PKH(addr), "Not a P2PKH address: " + addr);
        return addr;
    }

    private BigDecimal getUTXOAmount(List<UTXO> utxos) {
        BigDecimal result = BigDecimal.ZERO;
        for (UTXO utxo : utxos) {
            result = result.add(utxo.getAmount());
        }
        return result;
    }
    
    private String signTx(Tx tx) {
        List<String> privKeys = new ArrayList<>();
        for (TxInput txin : tx.getInputs()) {
            UTXO utxo = (UTXO) txin;
            String privKey = findAddress(utxo.getAddress()).getPrivKey();
            privKeys.add(privKey);
        }
        String rawTx = createRawTx(tx);
        return client.signRawTransaction(rawTx, adaptInputs(tx.getInputs()), privKeys);
    }

    private String createRawTx(Tx tx) {
        return client.createRawTransaction(adaptInputs(tx.getInputs()), adaptOutputs(tx.getOutputs()));
    }

    private Address findAddress(String address) {
        return addressses.stream().filter(a -> a.getAddress().equals(address)).findFirst().orElse(null);
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

    class DefaultAddress implements Address {

        private final String address;
        private final boolean watchOnly;
        private final List<String> labels = new ArrayList<>();

        public DefaultAddress(String address, boolean watchOnly, List<String> labels) {
            this.address = address;
            this.watchOnly = watchOnly;
            this.labels.addAll(labels);
        }

        @Override
        public String getPrivKey() {
            return client.dumpPrivKey(address);
        }

        @Override
        public String getAddress() {
            return address;
        }

        @Override
        public boolean isWatchOnly() {
            return watchOnly;
        }

        @Override
        public void addLabels(List<String> labels) {
            for (String label : labels) {
                AssertArgument.assertFalse(this.labels.contains(label), "Duplicate label");
            }
            for (String label : labels) {
                this.labels.add(label);
            }
        }

        @Override
        public void addLabel(String label) {
            AssertArgument.assertFalse(labels.contains(label), "Duplicate label");
            labels.add(label);
        }

        @Override
        public void removeLabel(String label) {
            labels.remove(label);
        }

        @Override
        public List<String> getLabels() {
            return Collections.unmodifiableList(labels);
        }

        @Override
        public String toString() {
            return String.format("addr=%s, ro=%b, labels=%s", address, watchOnly, labels);
        }
    }

}
