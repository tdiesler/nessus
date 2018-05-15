package io.nessus.test.bitcoin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class WalletTest extends AbstractRegtestTest {

    @Test
    public void testSimpleSpending () throws Exception {

        BigDecimal btcMiner = getDefaultBalance();
        Assert.assertTrue(new BigDecimal("50.0").compareTo(btcMiner) <= 0);

        String addrBob = getAddress(ACCOUNT_BOB, 0);
        client.sendToAddress(addrBob, 10.0);
        
        List<String> blocks = generate(1);
        Assert.assertEquals(1, blocks.size());
        
        Map<String, Number> accounts = client.listAccounts();
        Assert.assertEquals(2, accounts.size());
        
        Double btcDefault = accounts.get(ACCOUNT_DEFAULT).doubleValue();
        Assert.assertTrue(50.0 < btcDefault && btcDefault < 100.0);
        
        Double btcBob = accounts.get(ACCOUNT_BOB).doubleValue();
        Assert.assertEquals(10.0, btcBob, 0);
    }
}
