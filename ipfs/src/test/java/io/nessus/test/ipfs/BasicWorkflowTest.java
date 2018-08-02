package io.nessus.test.ipfs;

import static io.nessus.Wallet.ALL_FUNDS;
import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.io.BufferedReader;

/*-
 * #%L
 * Nessus :: IPFS
 * %%
 * Copyright (C) 2018 Nessus
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.bitcoin.BitcoinBlockchain;
import io.nessus.ipfs.ContentManager;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.impl.CmdLineIPFSClient;
import io.nessus.ipfs.impl.DefaultContentManager;
import io.nessus.testing.AbstractBlockchainTest;

public class BasicWorkflowTest extends AbstractBlockchainTest {

    static ContentManager cntmgr;
    static Blockchain blockchain;
    static Network network;
    static Wallet wallet;
    
    Address addrBob;
    Address addrMarry;
    
    @BeforeClass
    public static void beforeClass() throws IOException {
        
        blockchain = BlockchainFactory.getBlockchain(DEFAULT_JSONRPC_REGTEST_URL, BitcoinBlockchain.class);
        IPFSClient ipfs = new CmdLineIPFSClient();

        cntmgr = new DefaultContentManager(ipfs, blockchain);
        network = blockchain.getNetwork();
        wallet = blockchain.getWallet();
        
        importAddresses(wallet, BasicWorkflowTest.class);
        
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
    
    @Test
    public void basicWorkflow() throws Exception {
        
        Long timeout = 4000L;
        
        // Register the public encryption keys
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        Address addrMarry = wallet.getAddress(LABEL_MARRY);
        
        PublicKey pubKeyBob = cntmgr.register(addrBob);
        assertKeyEquals(pubKeyBob, cntmgr.findRegistation(addrBob));
        
        PublicKey pubKeyMarry = cntmgr.register(addrMarry);
        assertKeyEquals(pubKeyMarry, cntmgr.findRegistation(addrMarry));
        
        // Add content to IPFS
        
        Path relPath = Paths.get("bob/userfile.txt");
        InputStream input = getClass().getResourceAsStream("/userfile.txt");
        FHandle fhandle = cntmgr.add(addrBob, input, relPath);
        Assert.assertTrue(new File(fhandle.getURL().toURI()).exists());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrBob, fhandle.getOwner());
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getCid());
        Assert.assertNotNull(fhandle.getTxId());
        
        // Verify local file content
        
        List<FHandle> fhandles = cntmgr.findLocalContent(addrBob);
        Assert.assertEquals(1, fhandles.size());
        FHandle fhLocal = fhandles.get(0);
        Assert.assertEquals(relPath, fhLocal.getPath());
        Assert.assertTrue(fhLocal.isAvailable());
        Assert.assertFalse(fhLocal.isExpired());
        Assert.assertFalse(fhLocal.isEncrypted());
        
        InputStream reader = cntmgr.getLocalContent(addrBob, relPath);
        BufferedReader br = new BufferedReader(new InputStreamReader(reader));
        Assert.assertEquals("The quick brown fox jumps over the lazy dog.", br.readLine());
        
        Assert.assertTrue(cntmgr.deleteLocalContent(addrBob, relPath));
        Assert.assertTrue(cntmgr.findLocalContent(addrBob).isEmpty());
        
        // Find IPFS content on blockchain
        
        String cidBob = fhandle.getCid();
        fhandle = findIPFSContent(addrBob, cidBob, timeout);
        Assert.assertTrue(fhandle.isAvailable());
        Assert.assertFalse(fhandle.isExpired());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrBob, fhandle.getOwner());
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getTxId());
        
        // Get content from IPFS
        
        fhandle  = cntmgr.get(addrBob, cidBob, relPath, timeout);
        Assert.assertTrue(new File(fhandle.getURL().toURI()).exists());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrBob, fhandle.getOwner());
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNull(fhandle.getCid());
        
        // Send content to IPFS
        
        fhandle  = cntmgr.send(addrBob, cidBob, addrMarry, timeout);
        Assert.assertTrue(new File(fhandle.getURL().toURI()).exists());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrMarry, fhandle.getOwner());
        Assert.assertNotNull(fhandle.getCid());
        
        // Find IPFS content on blockchain
        
        String cidMarry = fhandle.getCid();
        fhandle = findIPFSContent(addrMarry, cidMarry, timeout);
        Assert.assertTrue(fhandle.isAvailable());
        Assert.assertFalse(fhandle.isExpired());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrMarry, fhandle.getOwner());
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getTxId());
        
        // Get content from IPFS
        
        relPath = Paths.get("marry/userfile.txt");
        fhandle  = cntmgr.get(addrMarry, fhandle.getCid(), relPath, timeout);
        Assert.assertTrue(new File(fhandle.getURL().toURI()).exists());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrMarry, fhandle.getOwner());
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNull(fhandle.getCid());
    }

    private FHandle findIPFSContent(Address addr, String cid, Long timeout) throws IOException, InterruptedException {
        
        List<FHandle> fhandles = cntmgr.findIPFSContent(addr, timeout);
        FHandle fhandle  = fhandles.stream().filter(fh -> fh.getCid().equals(cid)).findFirst().get();
        Assert.assertNotNull(fhandle);
        Assert.assertFalse(fhandle.isAvailable());
        
        for (int i = 0; i < 4 && !fhandle.isAvailable(); i++) {
            Thread.sleep(1000);
            fhandles = cntmgr.findIPFSContent(addr, timeout);
            fhandle  = fhandles.stream().filter(fh -> fh.getCid().equals(cid)).findFirst().get();
        }
        
        return fhandle;
    }

    private void assertKeyEquals(PublicKey exp, PublicKey was) {
        String encExp = Base64.getEncoder().encodeToString(exp.getEncoded());
        String encWas = Base64.getEncoder().encodeToString(was.getEncoded());
        Assert.assertEquals(encExp, encWas);
    }
    
    @SuppressWarnings("serial")
    static class KnownPubKey implements PublicKey {

        final String encodedKey;
        
        KnownPubKey(String encodedKey) {
            this.encodedKey = encodedKey;
        }

        @Override
        public String getFormat() {
            return "X.509";
        }

        @Override
        public byte[] getEncoded() {
            return Base64.getDecoder().decode(encodedKey);
        }

        @Override
        public String getAlgorithm() {
            return "EC";
        }
    }
}
