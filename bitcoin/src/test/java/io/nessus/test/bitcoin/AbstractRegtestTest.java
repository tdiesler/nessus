package io.nessus.test.bitcoin;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Network;
import io.nessus.Wallet;
import io.nessus.bitcoin.BitcoinRegtestNetwork;
import io.nessus.bitcoin.BitcoinWallet;
import io.nessus.test.bitcoin.dto.Config;

public abstract class AbstractRegtestTest {

    final Logger LOG = LoggerFactory.getLogger(getClass());

    static final BigDecimal NETWORK_FEE = new BigDecimal("0.001");
    
    static final String LABEL_BOB = "Bob";
    static final String LABEL_MARRY = "Marry";
    static final String LABEL_SINK = "Sink";
    static final String LABEL_DEFAULT = "_default";
    static final String LABEL_CHANGE = "_change";
    
    private static final Network network = new BitcoinRegtestNetwork();
    private static final Wallet wallet = new BitcoinWallet();

    @BeforeClass
    public static void beforeClass() throws IOException {

        // Wallet already initialized
        if (!wallet.getLabels().isEmpty()) return;
        
        Config config = Config.parseConfig("/initial-import.json");
        for (Config.Address addr : config.getWallet().getAddresses()) {
            String privKey = addr.getPrivKey();
            String pubKey = addr.getPubKey();
            List<String> labels = addr.getLabels();
            if (privKey != null && pubKey == null) {
                wallet.addPrivateKey(privKey, labels);
            } else {
                wallet.addAddress(pubKey, labels);
            }
        }
        
        // Import the configured addresses and generate a few coins
        BigDecimal balanceA = wallet.getBalance("");
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

    public BigDecimal estimateFee() {
        return NETWORK_FEE;
    }
    
    public BigDecimal addFee(BigDecimal amount) {
        return amount.add(estimateFee());
    }
    
    public BigDecimal subtractFee(BigDecimal amount) {
        return amount.subtract(estimateFee());
    }
    
    void showAccountBalances() {
        for (String label : wallet.getLabels()) {
            if (!label.startsWith("_")) {
                BigDecimal val = wallet.getBalance(label);
                LOG.info(String.format("%-5s: %13.8f", label, val));
            }
        }
    }
}
