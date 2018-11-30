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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.ContentManager.Config;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.NessusUserFault;
import io.nessus.ipfs.impl.DefaultContentManager.FHeader;
import io.nessus.utils.FileUtils;
import io.nessus.utils.TimeUtils;

public class ContentManagerTest extends AbstractWorkflowTest {

    long timeout = 2000L;
    int attempts = 5;

    @Before
    public void before() throws Exception {
        super.before();
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        PublicKey pubKey = cntmgr.findAddressRegistation(addrBob);
        
        if (pubKey == null) {
            cntmgr.registerAddress(addrBob);
            pubKey = cntmgr.findAddressRegistation(addrBob);
        }
        
        Assert.assertNotNull(pubKey);
    }
    
    @Test
    public void unregisterAddress() throws Exception {

        Address addrBob = wallet.getAddress(LABEL_BOB);
        PublicKey pubKey = cntmgr.findAddressRegistation(addrBob);
        
        PublicKey resKey = cntmgr.unregisterAddress(addrBob);
        Assert.assertEquals(pubKey, resKey);
        
        pubKey = cntmgr.findAddressRegistation(addrBob);
        Assert.assertNull(pubKey);
    }
    
    @Test
    public void unregisterIPFS() throws Exception {

        Address addrBob = wallet.getAddress(LABEL_BOB);
        
        ByteArrayInputStream input = new ByteArrayInputStream("Hello Kermit".getBytes());
        FHandle fhandle = cntmgr.addIpfsContent(addrBob, Paths.get("some.txt"), input);
        
        List<FHandle> fhandles = cntmgr.findIpfsContent(addrBob, null);
        Assert.assertEquals(1, fhandles.size());
        Assert.assertEquals(fhandle, fhandles.get(0));
        
        List<String> cids = cntmgr.unregisterIpfsContent(addrBob, Arrays.asList(fhandle.getCid()));
        Assert.assertEquals(1, cids.size());
        Assert.assertEquals(fhandle.getCid(), cids.get(0));
        
        fhandles = cntmgr.findIpfsContent(addrBob, null);
        Assert.assertEquals(0, fhandles.size());
    }
    
    @Test
    public void simpleAdd() throws Exception {

        Path path = Paths.get("contentA/subA/file01.txt");
        cntmgr.removeLocalContent(addrBob, path);
        
        FHandle fhres = cntmgr.findLocalContent(addrBob, path);
        Assert.assertNull(fhres);
        
        InputStream input = new ByteArrayInputStream("file 01".getBytes());
        fhres = cntmgr.addIpfsContent(addrBob, path, input);
        Assert.assertTrue(fhres.isAvailable());
        Assert.assertTrue(fhres.isEncrypted());
        
        String cid = fhres.getCid();
        Assert.assertNotNull(cid);

        // Expect to find the local content
        fhres = cntmgr.findLocalContent(addrBob, path);
        Assert.assertTrue(fhres.isAvailable());
        
        // Unnecessary delay when adding encrypted content
        // https://github.com/tdiesler/nessus/issues/41
        
        // Use an extremely short timeout 
        fhres = findIpfsContent(addrBob, cid, 10L);
        Assert.assertTrue(fhres.isAvailable());
        
        // Clear the file cache & local file
        cntmgr.clearFileCache();
        cntmgr.removeLocalContent(addrBob, path);
        
        // Get the file from IPFS
        fhres = cntmgr.getIpfsContent(addrBob, cid, path, null);
        Assert.assertTrue(fhres.isAvailable());

        // Verify local content
        Reader rd = new InputStreamReader(cntmgr.getLocalContent(addrBob, path));
        Assert.assertEquals("file 01", new BufferedReader(rd).readLine());
    }

    @Test
    public void multipleAdd() throws Exception {

        Path path = Paths.get("contentA");
        cntmgr.removeLocalContent(addrBob, path);
        
        FHandle fhres = cntmgr.findLocalContent(addrBob, path);
        Assert.assertNull(fhres);
        
        Path srcPath = Paths.get("src/test/resources/contentA");
        Path dstPath = cntmgr.getPlainPath(addrBob).resolve("contentA");
        FileUtils.recursiveCopy(srcPath, dstPath);
        
        fhres = cntmgr.findLocalContent(addrBob, path);
        Assert.assertTrue(fhres.isAvailable());
        
        fhres = cntmgr.addIpfsContent(addrBob, path);
        Assert.assertTrue(fhres.isAvailable());
        Assert.assertTrue(fhres.isEncrypted());
        
        String cid = fhres.getCid();
        Assert.assertNotNull(cid);
        
        // Expect to find the local content
        fhres = cntmgr.findLocalContent(addrBob, path);
        Assert.assertTrue(fhres.isAvailable());
        
        // Clear the file cache & local file
        cntmgr.clearFileCache();
        cntmgr.removeLocalContent(addrBob, path);
        
        // Find the IPFS tree
        fhres = findIpfsContent(addrBob, cid, null);
        Assert.assertTrue(fhres.isAvailable());
        Assert.assertEquals(3, fhres.getChildren().size());
        Assert.assertEquals(cid + "/subA", fhres.getChildren().get(0).getCid());
        Assert.assertEquals(cid + "/subB", fhres.getChildren().get(1).getCid());
        Assert.assertEquals(cid + "/file03.txt", fhres.getChildren().get(2).getCid());
        
        // Get the file from IPFS
        fhres = cntmgr.getIpfsContent(addrBob, cid, null, null);
        Assert.assertTrue(fhres.isAvailable());
        Assert.assertEquals(3, fhres.getChildren().size());

        // Verify local content
        path = Paths.get("contentA/subA/file01.txt");
        Reader rd = new InputStreamReader(cntmgr.getLocalContent(addrBob, path));
        Assert.assertEquals("file 01", new BufferedReader(rd).readLine());
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
        // https://github.com/tdiesler/nessus/issues/47
        
        Path srcPath = Paths.get("some space");
        FHandle fhandle = cntmgr.addIpfsContent(addrBob, srcPath, input);
        Path cryptPath = fhandle.getFilePath();
        try (Reader rd = new FileReader(cryptPath.toFile())) {
            FHeader fheader = cntmgr.readFHeader(rd);
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

        createContentManager(new Config(blockchain, ipfsClient)
                .ipfsTimeout(timeout)
                .ipfsAttempts(attempts));
        
        Date start = new Date();
        addIpfsContent(addrBob, getTestPath(100), "test100_" + start.getTime());
        LOG.info("addContent: {}", TimeUtils.elapsedTimeString(start));

        start = new Date();
        List<FHandle> fhandles = cntmgr.findIpfsContent(addrBob, 20000L);
        LOG.info("findIPFSContent: {}", TimeUtils.elapsedTimeString(start));
        fhandles.forEach(fh -> LOG.info("{}", fh));
    }

    @Test
    public void spendFileRegs() throws Exception {

        createContentManager(new Config(blockchain, ipfsClient)
                .ipfsTimeout(timeout)
                .ipfsAttempts(attempts));
        
        cntmgr.removeLocalContent(addrBob, Paths.get("contentA"));
        
        List<FHandle> fhandles = cntmgr.findIpfsContent(addrBob, null);
        if (fhandles.isEmpty()) {
            addIpfsContent(addrBob, Paths.get("contentA/subA/file01.txt"));
            addIpfsContent(addrBob, Paths.get("contentA/subB/subC/file02.txt"));
            addIpfsContent(addrBob, Paths.get("contentA/file03.txt"));
        }
        
        fhandles = cntmgr.findIpfsContent(addrBob, null);
        fhandles.forEach(fh -> LOG.info("{}", fh));
        Assert.assertEquals(3, fhandles.size());
        
        awaitFileAvailability(fhandles, 3, true);
        
        // Spend Bob's file registrations
        unlockFileRegistrations(addrBob);
        wallet.sendFromLabel(LABEL_BOB, addrBob.getAddress(), Wallet.ALL_FUNDS);
        
        // Verify that no IPFS files are found
        fhandles = cntmgr.findIpfsContent(addrBob, null);
        Assert.assertTrue(fhandles.isEmpty());
    }

    @Test
    public void findNonExisting() throws Exception {

        createContentManager(new Config(blockchain, ipfsClient)
                .ipfsTimeout(timeout)
                .ipfsAttempts(attempts));
        
        List<FHandle> fhandles = cntmgr.findIpfsContent(addrBob, null);
        if (fhandles.isEmpty()) {
            long millis = System.currentTimeMillis();
            addIpfsContent(addrBob, getTestPath(200), "test200_" + millis);
            addIpfsContent(addrBob, getTestPath(201), "test201_" + millis);
            addIpfsContent(addrBob, getTestPath(202), "test202_" + millis);
        }
        
        fhandles = cntmgr.findIpfsContent(addrBob, null);
        fhandles = awaitFileAvailability(fhandles, 3, true);
        
        // SET A BREAKPOINT HERE AND CONTINUE WITH A NEW IPFS INSTANCE
        
        createContentManager(new Config(blockchain, ipfsClient)
                .ipfsTimeout(timeout)
                .ipfsAttempts(attempts));
        
        fhandles = cntmgr.findIpfsContent(addrBob, null);
        fhandles.forEach(fh -> LOG.info("{}", fh));
        
        // Verify that no IPFS files are found
        
        fhandles = awaitFileAvailability(fhandles, 1, false);
        if (fhandles.size() == 0) {
            long millis = System.currentTimeMillis();
            addIpfsContent(addrBob, getTestPath(200), "test200_" + millis);
            addIpfsContent(addrBob, getTestPath(201), "test201_" + millis);
            addIpfsContent(addrBob, getTestPath(202), "test202_" + millis);
        }
        
        fhandles = cntmgr.findIpfsContent(addrBob, null);
        fhandles.forEach(fh -> LOG.info("{}", fh));
        
        awaitFileAvailability(fhandles, 3, true);
    }

    @Test
    public void preventSilentOverwrite() throws Exception {

        Path path = Paths.get("override");
        cntmgr.removeLocalContent(addrBob, path);
        
        InputStream input = new ByteArrayInputStream("some text".getBytes());
        String cid = cntmgr.addIpfsContent(addrBob, path, input).getCid();

        // Verify that we cannot add to an existing path
        try {
            input = new ByteArrayInputStream("some other".getBytes());
            cntmgr.addIpfsContent(addrBob, path, input).getCid();
            Assert.fail("NessusUserFault expected");
        } catch (NessusUserFault ex) {
            Assert.assertTrue(ex.getMessage().contains("already exists"));
        }

        // Clear the file cache & local file
        cntmgr.clearFileCache();
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
        
        createContentManager(new Config(blockchain, ipfsClient).replaceExisting());

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

/*

killall ipfs
 
rm -rf ~/.bitcoin/regtest
rm -rf ~/.ipfs; ipfs init; ipfs daemon &
rm -rf ~/.fman

*/
