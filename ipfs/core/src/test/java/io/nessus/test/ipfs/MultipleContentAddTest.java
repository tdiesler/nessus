package io.nessus.test.ipfs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.ipfs.AHandle;
import io.nessus.ipfs.CidPath;
import io.nessus.ipfs.FHandle;
import io.nessus.utils.FileUtils;

public class MultipleContentAddTest extends AbstractIpfsTest {

    @Test
    public void multipleAdd() throws Exception {

        // Give Bob some funds
    	
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("1.0"));
        
        AHandle ahandle = cntmgr.registerAddress(addrBob);
        Assert.assertNotNull(ahandle.isAvailable());

        // Remove the local content
        
        Path path = Paths.get("contentA");
        cntmgr.removeLocalContent(addrBob, path);
        
        // Verify that the local content got removed
        
        FHandle fhres = cntmgr.findLocalContent(addrBob, path);
        Assert.assertNull(fhres);
        
        // Copy test resources to local content

        Path srcPath = Paths.get("src/test/resources/contentA");
        Path dstPath = cntmgr.getPlainPath(addrBob).resolve("contentA");
        FileUtils.recursiveCopy(srcPath, dstPath);
        
        // Verify that the local content can be found
        
        fhres = cntmgr.findLocalContent(addrBob, path);
        Assert.assertTrue(fhres.isAvailable());
        
        // Add local content to IPFS
        
        fhres = cntmgr.addIpfsContent(addrBob, path);
        Assert.assertTrue(fhres.isAvailable());
        Assert.assertTrue(fhres.isEncrypted());
        
        CidPath cid = fhres.getCidPath();
		Assert.assertEquals("QmbHxAmpdP6UETKZdZ8bvCj24jVqJ3c6xTGUpdTdEmAEHd", cid.toString());
        
        List<FHandle> fhandles = flatFileTree(fhres, new ArrayList<>());
        fhandles.forEach(fh -> LOG.info("{}", fh));
        Assert.assertEquals(7, fhandles.size());
        
        // Clear the file cache & local file
        
        cntmgr.getIPFSCache().clear();
        cntmgr.removeLocalContent(addrBob, path);
        
        // Find the IPFS tree
        
        fhres = findIpfsContent(addrBob, cid.getCid(), null);
        Assert.assertTrue(fhres.isAvailable());
        Assert.assertTrue(fhres.isEncrypted());
        Assert.assertEquals(3, fhres.getChildren().size());
        Assert.assertEquals(cid.append("subA"), fhres.getChildren().get(0).getCidPath());
        Assert.assertEquals(cid.append("subB"), fhres.getChildren().get(1).getCidPath());
        Assert.assertEquals(cid.append("file03.txt"), fhres.getChildren().get(2).getCidPath());
        
        // Get the file from IPFS
        
        fhres = cntmgr.getIpfsContent(addrBob, cid.getCid(), null, null);
        Assert.assertTrue(fhres.isAvailable());
        Assert.assertEquals(3, fhres.getChildren().size());

        // Verify local content
        
        path = Paths.get("contentA/subA/file01.txt");
        Reader rd = new InputStreamReader(cntmgr.getLocalContent(addrBob, path));
        Assert.assertEquals("file 01", new BufferedReader(rd).readLine());
    }
}
