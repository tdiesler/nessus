package io.nessus.test.bitcoin;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Account;
import io.nessus.Wallet;

public class InitialImportTest extends AbstractRegtestTest {

    @Test
    public void testInitialImport () throws Exception {

        Wallet wallet = getWallet();
        
        List<String> names = wallet.getAccountNames();
        Assert.assertEquals(Arrays.asList(ACCOUNT_BOB, ACCOUNT_MARRY, ACCOUNT_SINK), names);
        
        for (String name : names) {
            Account acc = wallet.getAccount(name);
            List<String> addrs = acc.getAddresses();
            LOG.info(String.format("%-5s addr=%s", name, addrs));
            Assert.assertEquals("Three addresses", 3, addrs.size());
        }
    }
}
