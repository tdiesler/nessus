package io.nessus.bitcoin;

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;

import io.nessus.AbstractBlockchainTest;
import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.Wallet;

public abstract class AbstractBitcoinTest extends AbstractBlockchainTest {

    @BeforeClass
    public static void beforeClass() throws IOException {

        Blockchain blockchain = BlockchainFactory.getBlockchain(DEFAULT_JSONRPC_REGTEST_URL);
        Wallet wallet = blockchain.getWallet();
        
        importAddresses(wallet);
        
        // Import the configured addresses and generate a few coins
        BigDecimal balanceA = wallet.getBalance("");
        if (balanceA.doubleValue() == 0.0) {

            Network network = blockchain.getNetwork();
            List<String> blocks = network.generate(101, null);
            Assert.assertEquals(101, blocks.size());
        }
    }
}
