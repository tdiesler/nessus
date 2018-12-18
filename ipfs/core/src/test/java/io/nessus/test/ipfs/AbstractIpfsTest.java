package io.nessus.test.ipfs;

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

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;

import io.ipfs.multihash.Multihash;
import io.nessus.Wallet.Address;
import io.nessus.bitcoin.AbstractBitcoinTest;
import io.nessus.ipfs.Config;
import io.nessus.ipfs.Config.ConfigBuilder;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.FHandle.FHBuilder;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.core.AHandle;
import io.nessus.ipfs.core.DefaultContentManager;
import io.nessus.utils.FileUtils;

public class AbstractIpfsTest extends AbstractBitcoinTest {

	protected static DefaultContentManager cntmgr;
	protected static IPFSClient ipfsClient;
    
    @Before
    public void before() throws Exception {
    	super.before();

    	if (cntmgr == null) {
    		
            Config config = new ConfigBuilder()
            		.bcurl(DEFAULT_JSONRPC_REGTEST_URL)
            		.build();
            
    		cntmgr = createContentManager(config);
    	}
    }

    DefaultContentManager createContentManager(Config config) throws Exception {
        LOG.info("");
        
        cntmgr = new DefaultContentManager(config);
        ipfsClient = cntmgr.getIPFSClient();
        
        // Delete all local files
        Path rootPath = cntmgr.getRootPath();
        FileUtils.recursiveDelete(rootPath);
        
        // Give Bob & Mary some funds
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("1.0"));
        wallet.sendToAddress(addrMary.getAddress(), new BigDecimal("1.0"));
        generate(1, addrSink);
        
        return cntmgr;
    }
    
    void unlockAddressRegistrations(Address addr) {
        wallet.listLockUnspent(Arrays.asList(addr)).stream().forEach(utxo -> {
            AHandle ahandle = cntmgr.getAHandleFromTx(utxo, null);
            if (ahandle != null) wallet.lockUnspent(utxo, true);
        });
    }

    void unlockFileRegistrations(Address owner) {
        wallet.listLockUnspent(Arrays.asList(owner)).stream().forEach(utxo -> {
            FHandle fhandle = cntmgr.getFHandleFromTx(owner, utxo);
            if (fhandle != null) wallet.lockUnspent(utxo, true);
        });
    }

    Path getTestPath(int idx) {
        return Paths.get(getClass().getSimpleName(), String.format("file%03d.txt", idx));
    }
    
    FHandle addIpfsContent(Address addrBob, Path path) throws Exception {
        InputStream input = getClass().getResourceAsStream("/" + path);
        return cntmgr.addIpfsContent(addrBob, path, input);
    }

    FHandle addIpfsContent(Address addrBob, Path path, String content) throws Exception {
        InputStream input = new ByteArrayInputStream(content.getBytes());
        return cntmgr.addIpfsContent(addrBob, path, input);
    }

    FHandle getIpfsContent(Address owner, Multihash cid) throws Exception {
        FHandle fhandle = new FHBuilder(owner, cid).build();
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
    
    FHandle findIpfsContent(Address addr, Multihash cid, Long timeout) throws Exception {
        List<FHandle> fhandles = cntmgr.findIpfsContent(addr, timeout);
        FHandle fhandle  = fhandles.stream().filter(fh -> fh.getCid().equals(cid)).findFirst().get();
        return fhandle;
    }

    List<FHandle> flatFileTree(FHandle fh, List<FHandle> result) {
        result.add(fh);
        for (FHandle ch : fh.getChildren()) {
            flatFileTree(ch, result);
        }
        return result;
    }

    List<FHandle> awaitFileAvailability(List<FHandle> fhandles, int count, boolean assertAvailable) throws Exception {
        
        if (fhandles.isEmpty()) return fhandles;
        
        long timeout = cntmgr.getConfig().getIpfsTimeout();
        long nowTime = System.currentTimeMillis();
        long endTime = nowTime + timeout / 10;
        
        List<FHandle> avfiles = getAvailableFiles(fhandles);
        
        while (avfiles.size() < count && nowTime < endTime) {
            Thread.sleep(timeout);
            avfiles = getAvailableFiles(cntmgr.findIpfsContent(addrBob, null));
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
