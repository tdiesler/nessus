package io.nessus.test.bitcoin;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Network;
import io.nessus.Wallet;

public class WalletTest extends AbstractRegtestTest {

    @Test
    public void testSimpleSpending () throws Exception {

        Network network = getNetwork();
        Wallet wallet = getWallet();
        
        String addrBob = wallet.getAccount(ACCOUNT_BOB).getPrimaryAddress();
        String addrSink = wallet.getAccount(ACCOUNT_SINK).getPrimaryAddress();
        
        // Show account balances
        showAccountBalances();
        
        // Verify that the default account has some bitcoin
        BigDecimal btcMiner = wallet.getBalance(ACCOUNT_DEFAULT);
        Assert.assertTrue(new BigDecimal("50.0").compareTo(btcMiner) <= 0);
        
        // Verify that Bob has no funds
        BigDecimal btcBob = wallet.getBalance(ACCOUNT_BOB);
        Assert.assertEquals(BigDecimal.ZERO, btcBob);
        
        // Send 10 BTC to Bob
        wallet.sendToAddress(addrBob, new BigDecimal("10.0"));
        
        // Mine the next block
        network.mineBlocks(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has received 10 BTC
        btcBob = wallet.getBalance(ACCOUNT_BOB);
        Assert.assertEquals(10.0, btcBob.doubleValue(), 0);
        
        // Bob sends everything to the Sink  
        wallet.sendFromAccount(ACCOUNT_BOB, addrSink, minusFee(btcBob));
        
        // Mine next block
        network.mineBlocks(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has no funds
        btcBob = wallet.getBalance(ACCOUNT_BOB);
        Assert.assertEquals(BigDecimal.ZERO, btcBob);
    }
}

/*

> generate 101
 
> sendtoaddress 2Mxztt6zTZ7KPHMdPt8iYtSLxQ9FHAPJMns 10.0
 
> generate 1

> listunspent
[
  {
    "txid": "a3176cd0da293041e05e8031b4758fe4f98ed6de7011d84ae191e66e22d92c5a",
    "vout": 1,
    "address": "2N2h6P4ojH14ymqdgkFY9TK8dN9xUSqjLJZ",
    "redeemScript": "0014bf9348d7c45600904ca6fc43e3a534df7a9ebeaf",
    "scriptPubKey": "a914679d98cdd775fd436a646ab12aba9f4834120a8487",
    "amount": 39.99995560,
    "confirmations": 1,
    "spendable": true,
    "solvable": true,
    "safe": true
  },
  {
    "txid": "03349a57155d0888f70cbbaacdc150c68aed99d1019b883c937d3b9250f2da6e",
    "vout": 0,
    "address": "mzMxhsFzyBEGKVzs8bmji7VNb5vqVaUsoh",
    "account": "",
    "scriptPubKey": "76a914ceb6fd2ce0d9e1abbc8aa54b1326282420ace11a88ac",
    "amount": 50.00000000,
    "confirmations": 101,
    "spendable": true,
    "solvable": true,
    "safe": true
  }
]

> listunspent 1 1 '["2Mxztt6zTZ7KPHMdPt8iYtSLxQ9FHAPJMns"]'
[
]

> importprivkey cVfiZLCWbCm3SWoBAToaCoMuYJJjEw5cR6ifuWQY1a5wadXynGC2

> getaddressesbyaccount ""
[
  "n3ha6rJa8ZS7B4v4vwNWn8CnLHfUYXW1XE",
  "2Mxztt6zTZ7KPHMdPt8iYtSLxQ9FHAPJMns",
  "bcrt1q7d2dzgxmgu4kzpmvrlz306kcffw225ydq2jwm7"
]

> listunspent 1 1 '["2Mxztt6zTZ7KPHMdPt8iYtSLxQ9FHAPJMns"]'
[
  {
    "txid": "a3176cd0da293041e05e8031b4758fe4f98ed6de7011d84ae191e66e22d92c5a",
    "vout": 0,
    "address": "2Mxztt6zTZ7KPHMdPt8iYtSLxQ9FHAPJMns",
    "account": "",
    "redeemScript": "0014f354d120db472b61076c1fc517ead84a5ca5508d",
    "scriptPubKey": "a9143f1afb5d6159fb782dcbd7d0311278fa3b87eb0487",
    "amount": 10.00000000,
    "confirmations": 1,
    "spendable": true,
    "solvable": true,
    "safe": true
  }
]

*/
