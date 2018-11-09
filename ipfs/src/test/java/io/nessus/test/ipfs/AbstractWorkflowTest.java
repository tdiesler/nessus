package io.nessus.test.ipfs;

import static io.nessus.Wallet.ALL_FUNDS;
import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.bitcoin.BitcoinBlockchain;
import io.nessus.ipfs.ContentManager;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.impl.DefaultContentManager;
import io.nessus.ipfs.impl.DefaultIPFSClient;
import io.nessus.testing.AbstractBlockchainTest;

public class AbstractWorkflowTest extends AbstractBlockchainTest {

    static ContentManager cntmgr;
    static Blockchain blockchain;
    static Network network;
    static Wallet wallet;
    
    Address addrBob;
    Address addrMarry;
    
    @BeforeClass
    public static void beforeClass() throws IOException {
        
        blockchain = BlockchainFactory.getBlockchain(DEFAULT_JSONRPC_REGTEST_URL, BitcoinBlockchain.class);
        IPFSClient ipfs = new DefaultIPFSClient();

        cntmgr = new DefaultContentManager(ipfs, blockchain);
        network = blockchain.getNetwork();
        wallet = blockchain.getWallet();
        
        importAddresses(wallet, AbstractWorkflowTest.class);
        
        generate(blockchain);
        
        // Delete all local files
        
        Files.walkFileTree(((DefaultContentManager) cntmgr).getRootPath(), new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                path.toFile().delete();
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    @Before
    public void before() {

        addrBob = wallet.getAddress(LABEL_BOB);
        addrMarry = wallet.getAddress(LABEL_MARRY);
        
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("1.0"));
        wallet.sendToAddress(addrMarry.getAddress(), new BigDecimal("1.0"));
    }

    @After
    public void after() {
        
        // Unlock all UTXOs
        
        wallet.listLockUnspent(Arrays.asList(addrBob, addrMarry)).stream().forEach(utxo -> {
            wallet.lockUnspent(utxo, true);
        });

        // Bob & Marry send everything to the Sink  
        Address addrSink = wallet.getAddress(LABEL_SINK);
        wallet.sendFromLabel(LABEL_BOB, addrSink.getAddress(), ALL_FUNDS);
        wallet.sendFromLabel(LABEL_MARRY, addrSink.getAddress(), ALL_FUNDS);
        network.generate(1);
    }

    FHandle addContent(Address addrBob, String path) throws Exception {
        Path relPath = Paths.get(path);
        InputStream input = getClass().getResourceAsStream("/" + path);
        return cntmgr.add(addrBob, input, relPath);
    }
}
