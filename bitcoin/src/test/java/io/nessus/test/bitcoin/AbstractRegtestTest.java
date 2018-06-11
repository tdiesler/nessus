package io.nessus.test.bitcoin;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.Wallet;
import io.nessus.test.bitcoin.dto.Config;

public abstract class AbstractRegtestTest {

    final Logger LOG = LoggerFactory.getLogger(getClass());

    static final String LABEL_BOB = "Bob";
    static final String LABEL_MARRY = "Marry";
    static final String LABEL_SINK = "Sink";
    
    @BeforeClass
    public static void beforeClass() throws IOException {

        Blockchain blockchain = BlockchainFactory.getBlockchain();
        Wallet wallet = blockchain.getWallet();
        
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

            Network network = blockchain.getNetwork();
            List<String> blocks = network.mineBlocks(101, null);
            Assert.assertEquals(101, blocks.size());
        }
    }

    @Before
    public void before() {
    }

    BigDecimal estimateFee() {
        Blockchain blockchain = BlockchainFactory.getBlockchain();
        return blockchain.getNetwork().estimateFee();
    }
    
    BigDecimal addFee(BigDecimal amount) {
        return amount.add(estimateFee());
    }
    
    BigDecimal subtractFee(BigDecimal amount) {
        return amount.subtract(estimateFee());
    }
    
    void showAccountBalances() {
        Blockchain blockchain = BlockchainFactory.getBlockchain();
        Wallet wallet = blockchain.getWallet();
        for (String label : wallet.getLabels()) {
            if (!label.startsWith("_")) {
                BigDecimal val = wallet.getBalance(label);
                LOG.info(String.format("%-5s: %13.8f", label, val));
            }
        }
    }
}
