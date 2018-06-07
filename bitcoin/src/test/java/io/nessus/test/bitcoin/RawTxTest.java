package io.nessus.test.bitcoin;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Network;
import io.nessus.Wallet;

public class RawTxTest extends AbstractRegtestTest {

    @Test
    public void testSimpleSpending() throws Exception {

        Network network = getNetwork();
        Wallet wallet = getWallet();
        
        String addrBob = wallet.getDefaultAddress(LABEL_BOB).getAddress();
        String addrMarry = wallet.getDefaultAddress(LABEL_MARRY).getAddress();
        String addrSink = wallet.getDefaultAddress(LABEL_SINK).getAddress();
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has no funds
        BigDecimal btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(BigDecimal.ZERO, btcBob);
        
        // Verify that Marry has no funds
        BigDecimal btcMarry = wallet.getBalance(LABEL_MARRY);
        Assert.assertEquals(BigDecimal.ZERO, btcMarry);
        
        // Send 10 BTC to Bob
        BigDecimal tenBTC = new BigDecimal("10.0");
        wallet.sendToAddress(addrBob, tenBTC);
        
        // Mine next block
        network.mineBlocks(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Bob has received 10 BTC
        btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(tenBTC.doubleValue(), btcBob.doubleValue(), 0);
        
        // Bob sends 10-fees BTC to Marry  
        BigDecimal btcSend = subtractFee(tenBTC);
        wallet.sendFromLabel(LABEL_BOB, addrMarry, btcSend);
        
        // Mine next block
        network.mineBlocks(1);
        
        // Show account balances
        showAccountBalances();
        
        // Verify that Marry has received (almost) 10 BTC
        btcMarry = wallet.getBalance(LABEL_MARRY);
        Assert.assertEquals(btcSend.doubleValue(), btcMarry.doubleValue(), 0);
        
        // Verify that Bob has spent 10 BTC
        btcBob = wallet.getBalance(LABEL_BOB);
        Assert.assertEquals(BigDecimal.ZERO, btcBob);
        
        // Marry sends everything to the Sink  
        wallet.sendFromLabel(LABEL_MARRY, addrSink, subtractFee(btcMarry));
        
        // Mine next block
        network.mineBlocks(1);
        
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

/*
> generate 101

> importprivkey cVfiZLCWbCm3SWoBAToaCoMuYJJjEw5cR6ifuWQY1a5wadXynGC2 Bob 

> importprivkey cMcT5vjU5UiF2mh8WE6EsD3YdFEuFuUKm6rCcA8UuR9FUS6VLeRx Marry

> listaccounts
{
  "": 50.00000000,
  "Bob": 0.00000000,
  "Marry": 0.00000000
}

> getaddressesbyaccount Bob
[
  "n3ha6rJa8ZS7B4v4vwNWn8CnLHfUYXW1XE",
  "2Mxztt6zTZ7KPHMdPt8iYtSLxQ9FHAPJMns",
  "bcrt1q7d2dzgxmgu4kzpmvrlz306kcffw225ydq2jwm7"
]

> getaddressesbyaccount Marry
[
  "mm2PoHeFncAStYeZJSSTa4bmUVXRa3L6PL",
  "2NCvSGnUTueMFim31cpyw7yp1pKwAyeH4Vj",
  "bcrt1q835lks7r7s7qru78g7uh6l5apr46akde5z5zq2"
]

> sendtoaddress n3ha6rJa8ZS7B4v4vwNWn8CnLHfUYXW1XE 10.0
> generate 1
> listaccounts
{
  "": 89.99996200,
  "Bob": 10.00000000,
  "Marry": 0.00000000
}

> listunspent 1
[
  ...,
  {
    "txid": "c680f0abc596dfb5cec6dfc44a7ef7e54c2fa0b6557910c67cc4c3474487d37c",
    "vout": 0,
    "address": "n3ha6rJa8ZS7B4v4vwNWn8CnLHfUYXW1XE",
    "account": "Bob",
    "scriptPubKey": "76a914f354d120db472b61076c1fc517ead84a5ca5508d88ac",
    "amount": 10.00000000,
    "confirmations": 1,
    "spendable": true,
    "solvable": true,
    "safe": true
  },
  ...
]
> createrawtransaction '[{"txid":"c680f0abc596dfb5cec6dfc44a7ef7e54c2fa0b6557910c67cc4c3474487d37c","vout":0}]' '{"mm2PoHeFncAStYeZJSSTa4bmUVXRa3L6PL":9.99}'
02000000017cd3874447c3c47cc6107955b6a02f4ce5f77e4ac4dfc6ceb5df96c5abf080c60000000000ffffffff01c0878b3b000000001976a9143c69fb43c3f43c01f3c747b97d7e9d08ebaed9b988ac00000000

> signrawtransaction '02000000017cd3874447c3c47cc6107955b6a02f4ce5f77e4ac4dfc6ceb5df96c5abf080c60000000000ffffffff01c0878b3b000000001976a9143c69fb43c3f43c01f3c747b97d7e9d08ebaed9b988ac00000000' '[{"txid":"c680f0abc596dfb5cec6dfc44a7ef7e54c2fa0b6557910c67cc4c3474487d37c","vout":0, "scriptPubKey":"76a914f354d120db472b61076c1fc517ead84a5ca5508d88ac"}]' '["cVfiZLCWbCm3SWoBAToaCoMuYJJjEw5cR6ifuWQY1a5wadXynGC2"]'
{
  "hex": "02000000017cd3874447c3c47cc6107955b6a02f4ce5f77e4ac4dfc6ceb5df96c5abf080c6000000006a473044022009fcc3faa221db782d56f1e388979e61d5eb903ec0f16e919051aba631c3bdbf022073be7a8ea3ba73e436f6dd2f4b365131fe4537bbfa0944a73872e15e55f366eb01210389f32f5b1a8339b53308c4ee36bdcaacec27cf74e4138d2b372c8df323c21cf6ffffffff01c0878b3b000000001976a9143c69fb43c3f43c01f3c747b97d7e9d08ebaed9b988ac00000000",
  "complete": true
}

> sendrawtransaction 02000000017cd3874447c3c47cc6107955b6a02f4ce5f77e4ac4dfc6ceb5df96c5abf080c6000000006a473044022009fcc3faa221db782d56f1e388979e61d5eb903ec0f16e919051aba631c3bdbf022073be7a8ea3ba73e436f6dd2f4b365131fe4537bbfa0944a73872e15e55f366eb01210389f32f5b1a8339b53308c4ee36bdcaacec27cf74e4138d2b372c8df323c21cf6ffffffff01c0878b3b000000001976a9143c69fb43c3f43c01f3c747b97d7e9d08ebaed9b988ac00000000
> generate 1

> listunspent 1
[
  {
    "txid": "efcac0dca6a10e9f0a2a89ac34e64309819441dcc2f9553e1d3cf52a333ab939",
    "vout": 0,
    "address": "mqnLRnRoExhUFsP1Y66JLen2h2qXtCw8dF",
    "scriptPubKey": "210340323d67d27e075030ea9c286e078e30ec728b46710952d3e2bb201cca533b24ac",
    "amount": 50.00000000,
    "confirmations": 102,
    "spendable": true,
    "solvable": true,
    "safe": true
  },
  {
    "txid": "fa3a28d1f9136fadb43984e464276b6b2a28443af0a0f73381487a71204a4d44",
    "vout": 0,
    "address": "mm2PoHeFncAStYeZJSSTa4bmUVXRa3L6PL",
    "account": "Marry",
    "scriptPubKey": "76a9143c69fb43c3f43c01f3c747b97d7e9d08ebaed9b988ac",
    "amount": 9.99000000,
    "confirmations": 1,
    "spendable": true,
    "solvable": true,
    "safe": true
  },
  {
    "txid": "c680f0abc596dfb5cec6dfc44a7ef7e54c2fa0b6557910c67cc4c3474487d37c",
    "vout": 1,
    "address": "2NFAvRVEoE7jfVuZQkSgSNZRkEUeUeic3HR",
    "redeemScript": "001406a25b3828b5f9a58940279fb65c72fa3ebf8702",
    "scriptPubKey": "a914f08270c43a050a9faf69dc0bdd3b8e078a48c56987",
    "amount": 39.99996200,
    "confirmations": 2,
    "spendable": true,
    "solvable": true,
    "safe": true
  },
  {
    "txid": "8104c596910e143232dd3f6c94875fe42ef358908575bb87e93830d9db2c77d4",
    "vout": 0,
    "address": "mqnLRnRoExhUFsP1Y66JLen2h2qXtCw8dF",
    "scriptPubKey": "210340323d67d27e075030ea9c286e078e30ec728b46710952d3e2bb201cca533b24ac",
    "amount": 50.00000000,
    "confirmations": 101,
    "spendable": true,
    "solvable": true,
    "safe": true
  }
]

> listaccounts
{
  "": 129.99996200,
  "Bob": 10.00000000,
  "Marry": 9.99000000
}
*/
 