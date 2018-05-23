package io.nessus.test.bitcoin;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Network;
import io.nessus.Wallet;
import io.nessus.bitcoin.BitcoinRegtestNetwork;
import io.nessus.bitcoin.BitcoinWallet;

public abstract class AbstractRegtestTest {

    final Logger LOG = LoggerFactory.getLogger(getClass());

    static final BigDecimal NETWORK_FEE = new BigDecimal("0.001");
    
    static final String ACCOUNT_BOB = "Bob";
    static final String ACCOUNT_MARRY = "Marry";
    static final String ACCOUNT_SINK = "Sink";
    static final String ACCOUNT_DEFAULT = "";
    
    private static final Network network = new BitcoinRegtestNetwork();
    private static final Wallet wallet = new BitcoinWallet();

    @BeforeClass
    public static void beforeClass() throws IOException {

        // Import some private keys
        Properties props = new Properties();
        props.load(AbstractRegtestTest.class.getResourceAsStream("/initial-import.txt"));
        for (Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String privKey = (String) entry.getValue();
            if (key.endsWith(".privKey")) {
                String accName = key.substring(0, key.indexOf('.'));
                if (!wallet.getAccountNames().contains(accName)) {
                    wallet.createAccount(accName, privKey);
                }
            }
        }
        
        // Mine a few bitcoin
        BigDecimal balanceA = wallet.getBalance(ACCOUNT_DEFAULT);
        if (balanceA.doubleValue() == 0.0) {

            List<String> blocks = network.mineBlocks(101, null);
            Assert.assertEquals(101, blocks.size());
        }
    }

    @Before
    public void before() {
    }

    public Network getNetwork() {
        return network;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public BigDecimal minusFee(BigDecimal amount) {
        return amount.subtract(NETWORK_FEE);
    }
    
    int showAccountBalances() {
        List<String> accounts = new ArrayList<>();
        accounts.add("");
        accounts.addAll(wallet.getAccountNames());
        for (String acc : accounts) {
            BigDecimal val = wallet.getBalance(acc);
            LOG.info(String.format("%-5s: %13.8f", acc, val));
        }
        return accounts.size();
    }
}
