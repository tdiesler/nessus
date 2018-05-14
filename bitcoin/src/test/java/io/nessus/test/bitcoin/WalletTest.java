package io.nessus.test.bitcoin;

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import org.junit.Assert;
import org.junit.Test;

import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;

public class WalletTest {

    static final BitcoinJSONRPCClient client = new BitcoinJSONRPCClient(DEFAULT_JSONRPC_REGTEST_URL);
    
    @Test
    public void testGetBalance () throws Exception {
        Double balance = client.getBalance();
        Assert.assertEquals("0.0", "" + balance);
    }
}
