package io.nessus.test.bitcoin;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Wallet;
import io.nessus.Wallet.Address;

public class InitialImportTest extends AbstractRegtestTest {

    @Test
    public void testInitialImport () throws Exception {

        Wallet wallet = getWallet();
        
        List<String> labels = wallet.getLabels();
        Assert.assertEquals(Arrays.asList(LABEL_BOB, LABEL_MARRY, LABEL_SINK), labels);
        
        for (String label : labels) {
            List<Address> addrs = wallet.getAddresses(label);
            LOG.info(String.format("%-5s: addr=%s", label, addrs));
            Assert.assertEquals("One address", 1, addrs.size());
        }
    }
}
