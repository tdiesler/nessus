package io.nessus.test.bitcoin;

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;

public class WalletTest {

    static final BitcoinJSONRPCClient client = new BitcoinJSONRPCClient(DEFAULT_JSONRPC_REGTEST_URL);
    
    @Test
    public void testGetBalance () throws Exception {
        
        Double balance = client.getBalance();
        if (balance == 0.0) {
            
            List<String> generate = client.generate(101);
            Assert.assertEquals(101, generate.size());
        }

        balance = client.getBalance();
        Assert.assertEquals("50.0", "" + balance);
    }
}
