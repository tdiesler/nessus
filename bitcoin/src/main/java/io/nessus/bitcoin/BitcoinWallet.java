package io.nessus.bitcoin;

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Wallet;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.BasicTxInput;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.BasicTxOutput;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.TxInput;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.TxOutput;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.Unspent;

public class BitcoinWallet implements Wallet {

    static final Logger LOG = LoggerFactory.getLogger(BitcoinWallet.class);

    static final BitcoinJSONRPCClient client = new BitcoinJSONRPCClient(DEFAULT_JSONRPC_REGTEST_URL);
    
    private final Set<Address> addressses = new LinkedHashSet<>();

    class BitcoinAddress implements Address {

        private final String address;
        private final boolean watchOnly;
        private final List<String> labels = new ArrayList<>();
        
        public BitcoinAddress(String address, boolean watchOnly, List<String> labels) {
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
        public void addLabel(String label) {
            Assert.assertFalse("Duplicate label", labels.contains(label));
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
        
        BitcoinAddress addr = new BitcoinAddress(address, false, labels);
        addressses.add(addr);
        
        return addr;
    }

    @Override
    public Address addAddress(String address, List<String> labels) {

        // Note, this does not use the bitcoin-core account system 
        // as this will be removed in 0.18.0
        client.importAddress(address, "", true);
        
        BitcoinAddress addr = new BitcoinAddress(address, true, labels);
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
    public List<Address> getAddresses(String label) {
        return addressses.stream().filter(a -> a.getLabels().contains(label)).collect(Collectors.toList());
    }

    @Override
    public Address getDefaultAddress(String label) {
        List<Address> addrs = getAddresses(label);
        return addrs != null && addrs.size() > 0 ? addrs.iterator().next() : null;
    }

    @Override
    public Address getAddress(String address) {
        return addressses.stream().filter(a -> a.getAddress().equals(address)).findFirst().orElse(null);
    }

    @Override
    public List<Unspent> listUnspent(List<String> addrs) {
        return client.listUnspent(1, Integer.MAX_VALUE, addrs.toArray(new String[addrs.size()]));
    }

    @Override
    public BigDecimal getBalance(String label) {
        Double amount = 0.0;
        List<String> addrs = getPublicAddresses(label);
        for (Unspent utox : listUnspent(addrs)) {
            amount += utox.amount();
        }
        return amount == 0.0 ? BigDecimal.ZERO : new BigDecimal(String.format("%.8f", amount));
    }

    @Override
    public String sendToAddress(String toAddress, BigDecimal amount) {
        return client.sendToAddress(toAddress, amount.doubleValue());
    }

    @Override
    public String sendFromLabel(String label, String toAddress, BigDecimal amount) {
        
        BigDecimal balance = getBalance(label);
        Assert.assertTrue("Insufficient funds: " + balance, amount.compareTo(balance) <= 0);

        // Find the best UTOX to satisfy the amount  
        
        Unspent utox = null;
        Double utoxAmount = null;
        List<String> addrs = getPublicAddresses(label);
        for (Unspent auxUtox : listUnspent(addrs)) {
            if (amount.doubleValue() <= auxUtox.amount()) {
                if (utoxAmount == null || auxUtox.amount() < utoxAmount) {
                    utoxAmount = auxUtox.amount();
                    utox = auxUtox;
                    break;
                }
            }
        }
        Assert.assertNotNull("Cannot find UTOX with sufficient funds", utox);
        
        String txid = utox.txid();
        int vout = utox.vout();
        String scriptPubKey = utox.scriptPubKey();
        LOG.debug(String.format("%-5s: tx=%s vout=%d amt=%.8f", label, txid, vout, amount.doubleValue()));
        
        TxInput txIn = new BasicTxInput(txid, vout, scriptPubKey);
        List<TxInput> inputs = Arrays.asList(txIn);
        TxOutput txOut = new BasicTxOutput(toAddress, amount.doubleValue());
        List<TxOutput> outputs = Arrays.asList(txOut);
        String rawtx = client.createRawTransaction(inputs, outputs);
        LOG.debug("rawTx: {}", rawtx);

        String privKey = getAddress(utox.address()).getPrivKey();
        String sigTx = client.signRawTransaction(rawtx, inputs, Arrays.asList(privKey));
        LOG.debug("sigTx: {}", sigTx);
        
        txid = client.sendRawTransaction(sigTx);
        LOG.debug("txId: {}", txid);
        
        return txid;
    }

    private List<String> getPublicAddresses(String label) {
        return getAddresses(label).stream().map(a -> a.getAddress()).collect(Collectors.toList());
    }
}
