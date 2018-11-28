package io.nessus.test.ipfs.core;

/*-
 * #%L
 * Nessus :: IPFS :: Core
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

import static io.nessus.Wallet.ALL_FUNDS;
import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.bitcoin.BitcoinBlockchain;
import io.nessus.core.ipfs.FHandle;
import io.nessus.core.ipfs.FHandle.FHBuilder;
import io.nessus.core.ipfs.IPFSClient;
import io.nessus.core.ipfs.ContentManager.Config;
import io.nessus.core.ipfs.impl.DefaultContentManager;
import io.nessus.core.ipfs.impl.DefaultIPFSClient;
import io.nessus.core.ipfs.impl.ExtendedContentManager;
import io.nessus.testing.AbstractBlockchainTest;

public class AbstractWorkflowTest extends AbstractBlockchainTest {

    static ExtendedContentManager cntmgr;
    static IPFSClient ipfsClient;
    static Blockchain blockchain;
    static Network network;
    static Wallet wallet;
    
    Address addrBob;
    Address addrMary;
    
    @BeforeClass
    public static void abstractBeforeClass() throws Exception {
        
        blockchain = BlockchainFactory.getBlockchain(DEFAULT_JSONRPC_REGTEST_URL, BitcoinBlockchain.class);
        network = blockchain.getNetwork();
        wallet = blockchain.getWallet();
        
        ipfsClient = new DefaultIPFSClient();
        cntmgr = new ExtendedContentManager(new Config(blockchain, ipfsClient));
        
        importAddresses(wallet, AbstractWorkflowTest.class);
        
        generate(blockchain);
        
        // Delete all local files
        
        Path rootPath = cntmgr.getRootPath();
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                path.toFile().delete();
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    @Before
    public void before() throws Exception {

        addrBob = wallet.getAddress(LABEL_BOB);
        addrMary = wallet.getAddress(LABEL_MARY);
        
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("1.0"));
        wallet.sendToAddress(addrMary.getAddress(), new BigDecimal("1.0"));
    }

    @After
    public void after() {
        
        // Unlock all UTXOs
        List<Address> addresses = Arrays.asList(addrBob, addrMary);
        addresses.forEach(addr -> {
            unlockAddressRegistrations(addr);
            unlockFileRegistrations(addr);
        });

        // Bob & Mary send everything to the Sink  
        Address addrSink = wallet.getAddress(LABEL_SINK);
        wallet.sendFromLabel(LABEL_BOB, addrSink.getAddress(), ALL_FUNDS);
        wallet.sendFromLabel(LABEL_MARY, addrSink.getAddress(), ALL_FUNDS);
        network.generate(1);
    }

    DefaultContentManager createContentManager(long timeout, int attempts) {
        LOG.info("");
        
        Config config = new Config(blockchain, ipfsClient)
                .ipfsTimeout(timeout)
                .ipfsAttempts(attempts);
        
        return cntmgr = new ExtendedContentManager(config);
    }
    
    void unlockAddressRegistrations(Address addr) {
        wallet.listLockUnspent(Arrays.asList(addr)).stream().forEach(utxo -> {
            PublicKey pubKey = cntmgr.getPubKeyFromTx(utxo, null);
            if (pubKey != null) unlockAddressRegistration(addr, pubKey, utxo); 
        });
    }

    void unlockFileRegistrations(Address addr) {
        wallet.listLockUnspent(Arrays.asList(addr)).stream().forEach(utxo -> {
            FHandle fhandle = cntmgr.getFHandleFromTx(utxo, null);
            if (fhandle != null) unlockFileRegistration(addr, fhandle, utxo); 
        });
    }

    void unlockAddressRegistration(Address addr, PublicKey pubKey, UTXO utxo) {
        wallet.lockUnspent(utxo, true);
    }

    void unlockFileRegistration(Address addr, FHandle fhandle, UTXO utxo) {
        wallet.lockUnspent(utxo, true);
    }

    Path getTestPath(int idx) {
        return Paths.get(getClass().getSimpleName(), String.format("file%03d.txt", idx));
    }
    
    FHandle addContent(Address addrBob, Path path) throws Exception {
        InputStream input = getClass().getResourceAsStream("/" + path);
        return cntmgr.add(addrBob, input, path);
    }

    FHandle addContent(Address addrBob, Path path, String content) throws Exception {
        InputStream input = new ByteArrayInputStream(content.getBytes());
        return cntmgr.add(addrBob, input, path);
    }

    FHandle ipfsGet(Address owner, String cid) throws Exception {
        FHandle fhandle = new FHBuilder(cid).owner(owner).build();
        long ipfsTimeout = cntmgr.getConfig().getIpfsTimeout();
        try {
            long before = System.currentTimeMillis();
            Future<Path> future = ipfsClient.get(cid, Paths.get("target/" + cid));
            Path path = future.get(ipfsTimeout, TimeUnit.MILLISECONDS);
            long elapsed = System.currentTimeMillis() - before;
            fhandle = new FHBuilder(fhandle).path(path).available(true).elapsed(elapsed).build();
        } catch (TimeoutException ex) {
            // ignore
        }
        LOG.info("ipfsGet: {}", fhandle);
        return fhandle;
    }
    
    FHandle findIPFSContent(Address addr, String cid, Long timeout) throws Exception {
        
        List<FHandle> fhandles = cntmgr.findIPFSContent(addr, timeout);
        FHandle fhandle  = fhandles.stream().filter(fh -> fh.getCid().equals(cid)).findFirst().get();
        Assert.assertNotNull(fhandle);
        
        return fhandle;
    }

    List<FHandle> awaitFileAvailability(List<FHandle> fhandles, int count, boolean assertAvailable) throws Exception {
        
        if (fhandles.isEmpty()) return fhandles;
        
        long timeout = cntmgr.getConfig().getIpfsTimeout();
        long nowTime = System.currentTimeMillis();
        long endTime = nowTime + timeout / 10;
        
        List<FHandle> avfiles = getAvailableFiles(fhandles);
        
        while (avfiles.size() < count && nowTime < endTime) {
            Thread.sleep(timeout);
            avfiles = getAvailableFiles(cntmgr.findIPFSContent(addrBob, null));
            nowTime = System.currentTimeMillis();
        }
        
        if (assertAvailable)
            Assert.assertEquals(count, avfiles.size());
        
        return avfiles;
    }

    List<FHandle> getAvailableFiles(List<FHandle> fhandles) {
        return fhandles.stream().filter(fh -> fh.isAvailable()).collect(Collectors.toList());
    }
}
