package io.nessus.test.bitcoin;

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;

public abstract class AbstractRegtestTest {

    final Logger LOG = LoggerFactory.getLogger(getClass());

    static final String ACCOUNT_BOB = "Bob";
    static final String ACCOUNT_MARRY = "Marry";
    static final String ACCOUNT_DEFAULT = "";

    static final BitcoinJSONRPCClient client = new BitcoinJSONRPCClient(DEFAULT_JSONRPC_REGTEST_URL);

    @BeforeClass
    public static void beforeClass() {

        // Mine a few bitcoin
        BigDecimal balanceA = getDefaultBalance();
        if (balanceA.doubleValue() == 0.0) {

            List<String> blocks = generate(101);
            Assert.assertEquals(101, blocks.size());
        }
    }

    static List<String> generate(int numBlocks) {
        String addr = getDefaultAddress();
        return client.generateToAddress(numBlocks, addr);
    }

    static BigDecimal getDefaultBalance() {
        return getBalance(ACCOUNT_DEFAULT);
    }
    
    static BigDecimal getBalance(String account) {
        return new BigDecimal(String.format("%.8f", client.getBalance(account)));
    }
    
    static String getDefaultAddress() {
        return getAddress("", 0);
    }
    
    static String getDefaultAddress(String account) {
        return getAddress(account, 0);
    }
    
    static boolean hasAddress(String account, int idx) {
        List<String> addr = client.getAddressesByAccount(account);
        return 0 <= idx && idx < addr.size();
    }
    
    static String getAddress(String account, int idx) {
        while (!hasAddress(account, idx)) {
            client.getNewAddress(account);
        }
        List<String> addr = client.getAddressesByAccount(account);
        return addr.get(idx);
    }
}
