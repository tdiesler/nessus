package io.nessus.test.bitcoin;

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;

public class WalletTest {

    static final BitcoinJSONRPCClient client = new BitcoinJSONRPCClient(DEFAULT_JSONRPC_REGTEST_URL);
    
    @Test
    public void testSimpleSpending () throws Exception {

        BigDecimal balanceA = getBalance();
        if (balanceA.doubleValue() == 0.0) {
            
            List<String> blocks = client.generate(101);
            Assert.assertEquals(101, blocks.size());
        }

        BigDecimal balanceB = getBalance();
        Assert.assertTrue("Expected: " + balanceB + " <= 50.0", balanceB.compareTo(new BigDecimal("50.0")) <= 0);
        
        String newAddress = client.getNewAddress();
        String txId = client.sendToAddress(newAddress, 10.0);
        
        List<String> blocks = client.generate(1);
        Assert.assertEquals(1, blocks.size());
        
        BigDecimal balanceC = getBalance();
        Assert.assertTrue("Expected: " + balanceC + " < " + balanceB, balanceC.compareTo(balanceB) < 0);
    }

    private BigDecimal getBalance() {
        return new BigDecimal(String.format("%.8f", client.getBalance()));
    }
}
