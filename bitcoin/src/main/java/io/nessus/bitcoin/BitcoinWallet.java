package io.nessus.bitcoin;

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Account;
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
    
    private final Map<String, BitcoinAccount> accounts = new HashMap<>();

    @Override
    public void createAccount(String account, String privKey) {
        
        Assert.assertFalse("Duplicate account name", getAccountNames().contains(account));
        client.importPrivKey(privKey, "", true);
        
        // bitcoin-core -regtest v0.16.0 generates three public addresses for
        // every private key that gets imported. These are
        // #1 P2PKH  e.g. n3ha6rJa8ZS7B4v4vwNWn8CnLHfUYXW1XE
        // #2 P2SH   e.g. 2Mxztt6zTZ7KPHMdPt8iYtSLxQ9FHAPJMns
        // #3 Bech32 e.g. bcrt1q7d2dzgxmgu4kzpmvrlz306kcffw225ydq2jwm7
        for (String addr : client.getAddressesByAccount("")) {
            String auxKey = client.dumpPrivKey(addr);
            LOG.debug("{} => {}", auxKey, addr);
            
            // This associates the first address (only) with an account
            if (privKey.equals(auxKey)) {
                addAccount(account, addr);
                break;
            }
        }
    }

    @Override
    public List<String> getAccountNames() {
        return accounts.keySet().stream().sorted().collect(Collectors.toList());
    }

    @Override
    public Account getAccount(String name) {
        return accounts.get(name);
    }

    @Override
    public Account addAccount(String name, String address) {
        BitcoinAccount acc = new BitcoinAccount(name, address);
        accounts.put(acc.getName(), acc);
        return acc;
    }

    @Override
    public List<Unspent> listUnspent(List<String> addrs) {
        return client.listUnspent(1, Integer.MAX_VALUE, addrs.toArray(new String[addrs.size()]));
    }

    @Override
    public BigDecimal getBalance(String account) {
        Double amount = 0.0;
        if ("".equals(account)) {
            List<String> accAddrs = new ArrayList<>();
            accounts.values().stream().forEach(acc -> accAddrs.addAll(acc.getAddresses()));
            for (Unspent utox : client.listUnspent(1)) {
                if (!accAddrs.contains(utox.address())) {
                    amount += utox.amount();
                }
            }
        } else {
            Account acc = assertKnownAccount(account);
            List<String> addrs = acc.getAddresses();
            for (Unspent utox : listUnspent(addrs)) {
                amount += utox.amount();
            }
        }
        return amount == 0.0 ? BigDecimal.ZERO : new BigDecimal(String.format("%.8f", amount));
    }

    @Override
    public String sendToAddress(String toAddress, BigDecimal amount) {
        return client.sendToAddress(toAddress, amount.doubleValue());
    }

    @Override
    public String sendFromAccount(String account, String toAddress, BigDecimal amount) {
        
        Account acc = assertKnownAccount(account);
        BigDecimal balance = getBalance(account);
        Assert.assertTrue("Insufficient funds: " + balance, amount.compareTo(balance) <= 0);

        // Find the best UTOX to satisfy the amount  
        
        Unspent utox = null;
        Double utoxAmount = null;
        for (Unspent auxUtox : listUnspent(Arrays.asList(acc.getPrimaryAddress()))) {
            if (amount.doubleValue() <= auxUtox.amount()) {
                if (utoxAmount == null || auxUtox.amount() < utoxAmount) {
                    utoxAmount = auxUtox.amount();
                    utox = auxUtox;
                }
            }
        }
        Assert.assertNotNull("Cannot find single UTOX with sufficient funds", utox);
        
        String txid = utox.txid();
        int vout = utox.vout();
        String scriptPubKey = utox.scriptPubKey();
        LOG.debug(String.format("%-5s: tx=%s vout=%d amt=%.8f", account, txid, vout, amount.doubleValue()));
        
        TxInput txIn = new BasicTxInput(txid, vout, scriptPubKey);
        List<TxInput> inputs = Arrays.asList(txIn);
        TxOutput txOut = new BasicTxOutput(toAddress, amount.doubleValue());
        List<TxOutput> outputs = Arrays.asList(txOut);
        String rawtx = client.createRawTransaction(inputs, outputs);
        LOG.debug("rawTx: {}", rawtx);
        
        String sigTx = client.signRawTransaction(rawtx, inputs, Arrays.asList(acc.getPrivKey()));
        LOG.debug("sigTx: {}", sigTx);
        
        txid = client.sendRawTransaction(sigTx);
        LOG.debug("txId: {}", txid);
        
        return txid;
    }

    private Account assertKnownAccount(String account) {
        Account acc = getAccount(account);
        Assert.assertNotNull("Unknown account: " + account, acc);
        return acc;
    }
}
