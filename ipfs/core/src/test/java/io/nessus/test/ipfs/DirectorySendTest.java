package io.nessus.test.ipfs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.ipfs.CidPath;
import io.nessus.ipfs.FHandle;
import io.nessus.utils.FileUtils;

public class DirectorySendTest extends AbstractIpfsTest {

    @Test
    public void directorySend() throws Exception {

        // Give Bob some funds
    	
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("1.0"));
        wallet.sendToAddress(addrMary.getAddress(), new BigDecimal("1.0"));
        
        cntmgr.registerAddress(addrBob);
        cntmgr.registerAddress(addrMary);

        // Remove the local content
        
        Path path = Paths.get("contentA");
        cntmgr.removeLocalContent(addrBob, path);
        
        // Copy test resources to local content

        Path srcPath = Paths.get("src/test/resources/contentA");
        Path dstPath = cntmgr.getPlainPath(addrBob).resolve("contentA");
        FileUtils.recursiveCopy(srcPath, dstPath);
        
        // Add local content to IPFS
        
        FHandle fhres = cntmgr.addIpfsContent(addrBob, path);
		Assert.assertEquals(addrBob, fhres.getOwner());
        Assert.assertTrue(fhres.isAvailable());
        Assert.assertTrue(fhres.isEncrypted());
        
        CidPath cid = CidPath.parse("QmbHxAmpdP6UETKZdZ8bvCj24jVqJ3c6xTGUpdTdEmAEHd");
		Assert.assertEquals(cid, fhres.getCidPath());
             
		// Send directory to Mary
		
		fhres = cntmgr.sendIpfsContent(addrBob, cid.getCid(), addrMary, null);
		Assert.assertEquals(addrMary, fhres.getOwner());
        Assert.assertTrue(fhres.isAvailable());
        Assert.assertTrue(fhres.isEncrypted());
		
        cid = CidPath.parse("QmY97aAFBHdXM99nYTk6peP2mJpmmbVLws1j5i12Tp7tem");
		Assert.assertEquals(cid, fhres.getCidPath());
             
        // Get the file from IPFS
        
        fhres = cntmgr.getIpfsContent(addrMary, cid.getCid(), null, null);
		Assert.assertEquals(addrMary, fhres.getOwner());
        Assert.assertTrue(fhres.isAvailable());
        Assert.assertFalse(fhres.isEncrypted());
        Assert.assertEquals(3, fhres.getChildren().size());

        // Verify local content
        
        Path subpath = Paths.get("contentA/subA/file01.txt");
        Reader rd = new InputStreamReader(cntmgr.getLocalContent(addrMary, subpath));
        Assert.assertEquals("file 01", new BufferedReader(rd).readLine());
    }
}
