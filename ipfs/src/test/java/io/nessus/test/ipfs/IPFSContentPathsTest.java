package io.nessus.test.ipfs;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.nessus.Wallet.Address;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.impl.DefaultContentManager.FHeader;

public class IPFSContentPathsTest extends AbstractWorkflowTest {

    @Before
    public void before() throws Exception {
        super.before();
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        PublicKey pubKey = cntmgr.findRegistation(addrBob);
        
        if (pubKey == null) {
            cntmgr.register(addrBob);
            pubKey = cntmgr.findRegistation(addrBob);
        }
    }
    
    @Test
    public void testPaths() throws Exception {

        String content = "some text";
        InputStream input = new ByteArrayInputStream(content.getBytes());
        
        try {
            cntmgr.add(addrBob, input, null);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            // ignore
        }
        
        try {
            cntmgr.add(addrBob, input, Paths.get(""));
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            // ignore
        }
        
        try {
            cntmgr.add(addrBob, input, Paths.get(" "));
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            // ignore
        }
        
        // Investigate ipfs paths with whitespace
        // https://github.com/tdiesler/nessus/issues/47
        
        Path srcPath = Paths.get("some space");
        FHandle fhandle = cntmgr.add(addrBob, input, srcPath);
        Path cryptPath = Paths.get(fhandle.getURL().toURI());
        try (Reader rd = new FileReader(cryptPath.toFile())) {
            FHeader fheader = cntmgr.readFHeader(rd);
            Assert.assertEquals(srcPath, fheader.path);
        }
        Path destPath = Paths.get("some other");
        fhandle = cntmgr.decrypt(fhandle, addrBob, destPath, true);
        Path plainPath = Paths.get(fhandle.getURL().toURI());
        Assert.assertTrue(plainPath.toFile().isFile());
        Assert.assertEquals(destPath, plainPath.getFileName());
        try (Reader rd = new FileReader(plainPath.toFile())) {
            String line = new BufferedReader(rd).readLine();
            Assert.assertEquals(content, line);
        }
    }
}

