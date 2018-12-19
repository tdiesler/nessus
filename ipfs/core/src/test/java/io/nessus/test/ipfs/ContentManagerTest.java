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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.ipfs.multihash.Multihash;
import io.nessus.ipfs.AHandle;
import io.nessus.ipfs.ContentManagerConfig;
import io.nessus.ipfs.ContentManagerConfig.ContentManagerConfigBuilder;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.NessusUserFault;
import io.nessus.ipfs.core.FHeader;
import io.nessus.ipfs.core.FHeaderValues;
import io.nessus.utils.FileUtils;
import io.nessus.utils.TimeUtils;

public class ContentManagerTest extends AbstractIpfsTest {

    long timeout = 2000L;
    int attempts = 5;

    @Before
    public void before() throws Exception {
        super.before();
        
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("1.0"));
        
        AHandle ahandle = cntmgr.registerAddress(addrBob);
        Assert.assertNotNull(ahandle.isAvailable());
    }
    
    @Test
    public void contentPaths() throws Exception {

        String content = "some text";
        InputStream input = new ByteArrayInputStream(content.getBytes());
        
        try {
            cntmgr.addIpfsContent(addrBob, null, input);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            // ignore
        }
        
        try {
            cntmgr.addIpfsContent(addrBob, Paths.get(""), input);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            // ignore
        }
        
        try {
            cntmgr.addIpfsContent(addrBob, Paths.get(" "), input);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            // ignore
        }
        
        // Investigate ipfs paths with whitespace
        // https://github.com/jboss-fuse/nessus/issues/47
        
        Path srcPath = Paths.get("some space");
        FHandle fhandle = cntmgr.addIpfsContent(addrBob, srcPath, input);
        Path cryptPath = fhandle.getFilePath();
        try (Reader fr = new FileReader(cryptPath.toFile())) {
        	FHeaderValues fhv = cntmgr.getFHeaderValues();
            FHeader fheader = FHeader.fromReader(fhv, fr);
            Assert.assertEquals(srcPath, fheader.path);
        }
        Path destPath = Paths.get("some other");
        fhandle = cntmgr.decrypt(fhandle, destPath, true);
        Path plainPath = fhandle.getFilePath();
        Assert.assertTrue(plainPath.toFile().isFile());
        Assert.assertEquals(destPath, plainPath.getFileName());
        try (Reader rd = new FileReader(plainPath.toFile())) {
            String line = new BufferedReader(rd).readLine();
            Assert.assertEquals(content, line);
        }
    }
    @Test
    public void findTiming() throws Exception {

		ContentManagerConfig config = new ContentManagerConfigBuilder()
        		.bcurl(DEFAULT_JSONRPC_REGTEST_URL)
                .ipfsAttempts(attempts)
                .ipfsTimeout(timeout)
        		.build();
        
        createContentManager(config);
        
        Date start = new Date();
        addIpfsContent(addrBob, getTestPath(100), "test100_" + start.getTime());
        LOG.info("addContent: {}", TimeUtils.elapsedTimeString(start));

        start = new Date();
        List<FHandle> fhandles = cntmgr.findIpfsContent(addrBob, 20000L);
        LOG.info("findIPFSContent: {}", TimeUtils.elapsedTimeString(start));
        fhandles.forEach(fh -> LOG.info("{}", fh));
    }

    @Test
    public void preventOverwrite() throws Exception {

        Path path = Paths.get("override");
        cntmgr.removeLocalContent(addrBob, path);
        
        InputStream input = new ByteArrayInputStream("some text".getBytes());
        Multihash cid = cntmgr.addIpfsContent(addrBob, path, input).getCid();

        // Verify that we cannot add to an existing path
        
        try {
            input = new ByteArrayInputStream("some other".getBytes());
            cntmgr.addIpfsContent(addrBob, path, input).getCid();
            Assert.fail("NessusUserFault expected");
        } catch (NessusUserFault ex) {
            Assert.assertTrue(ex.getMessage().contains("already exists"));
        }

        // Clear the file cache & local file
        
        cntmgr.getIPFSCache().clear();
        cntmgr.removeLocalContent(addrBob, path);
        
        // Get the file from IPFS
        
        FHandle fhres = cntmgr.getIpfsContent(addrBob, cid, path, null);
        Assert.assertTrue(fhres.isAvailable());

        // Verify local content
        
        Reader rd = new InputStreamReader(cntmgr.getLocalContent(addrBob, path));
        Assert.assertEquals("some text", new BufferedReader(rd).readLine());

        // Verify that we cannot get to an existing path
        
        try {
            cntmgr.getIpfsContent(addrBob, cid, path, null);
            Assert.fail("NessusUserFault expected");
        } catch (NessusUserFault ex) {
            Assert.assertTrue(ex.getMessage().contains("already exists"));
        }
        
        // Use a content manager that allows overwrite
        
		ContentManagerConfig config = new ContentManagerConfigBuilder()
        		.bcurl(DEFAULT_JSONRPC_REGTEST_URL)
        		.overwrite(true)
        		.build();
        
        createContentManager(config);

        // Verify that we now can get to an existing path
        
        fhres = cntmgr.getIpfsContent(addrBob, cid, path, null);
        Assert.assertTrue(fhres.isAvailable());
        
        // Verify local content
        
        rd = new InputStreamReader(cntmgr.getLocalContent(addrBob, path));
        Assert.assertEquals("some text", new BufferedReader(rd).readLine());
    }


    @Test
    public void recursiveRemove() throws Exception {

        FileUtils.recursiveDelete(Paths.get("remove"));
        
        Path path = Paths.get("remove/file01.txt");
        InputStream input = new ByteArrayInputStream("some text".getBytes());
        cntmgr.addIpfsContent(addrBob, path, input).getCid();

        // Verify local content
        
        Reader rd = new InputStreamReader(cntmgr.getLocalContent(addrBob, path));
        Assert.assertEquals("some text", new BufferedReader(rd).readLine());
        
        Path fullPath = cntmgr.getPlainPath(addrBob).resolve(path);
        Assert.assertTrue(fullPath.toFile().isFile());
        
        Assert.assertTrue(cntmgr.removeLocalContent(addrBob, path));
        
        Assert.assertFalse(fullPath.toFile().exists());
        Assert.assertFalse(fullPath.getParent().toFile().exists());
    }
}
