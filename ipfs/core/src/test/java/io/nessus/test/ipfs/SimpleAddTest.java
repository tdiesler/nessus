package io.nessus.test.ipfs;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.ipfs.AHandle;
import io.nessus.ipfs.CidPath;
import io.nessus.ipfs.FHandle;
import io.nessus.utils.StreamUtils;

public class SimpleAddTest extends AbstractIpfsTest {

    @Test
    public void simpleAdd() throws Exception {

        // Give Bob some funds
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("1.0"));
        
        AHandle ahandle = cntmgr.registerAddress(addrBob);
        Assert.assertNotNull(ahandle.isAvailable());

        Path path = Paths.get("contentC/userfile.txt");
        cntmgr.removeLocalContent(addrBob, path);
        
        FHandle fhres = cntmgr.findLocalContent(addrBob, path);
        Assert.assertNull(fhres);
        
        InputStream ins = getClass().getResourceAsStream("/" + path);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamUtils.copyStream(ins, baos);
        
        ins = new ByteArrayInputStream(baos.toByteArray());
        fhres = cntmgr.addIpfsContent(addrBob, path, ins);
        Assert.assertTrue(fhres.isAvailable());
        Assert.assertTrue(fhres.isEncrypted());
        
        CidPath cid = CidPath.parse("Qmce1N5pky6LSVP37a8Hfv2kT7H6w1a1RyDbkaDRg6SSdS");
		Assert.assertEquals(cid, fhres.getCidPath());

        // Expect to find the local content
        
        fhres = cntmgr.findLocalContent(addrBob, path);
        Assert.assertTrue(fhres.isAvailable());
        
        // Unnecessary delay when adding encrypted content
        // https://github.com/jboss-fuse/nessus/issues/41
        
        // Use an extremely short timeout 
        
        fhres = findIpfsContent(addrBob, cid.getCid(), 10L);
        Assert.assertTrue(fhres.isAvailable());
        
        // Clear the file cache & local file
        
        cntmgr.getIPFSCache().clear();
        cntmgr.removeLocalContent(addrBob, path);
        
        // Get the file from IPFS
        
        fhres = cntmgr.getIpfsContent(addrBob, cid.getCid(), path, null);
        Assert.assertTrue(fhres.isAvailable());

        // Verify local content
        
        Reader rd = new InputStreamReader(cntmgr.getLocalContent(addrBob, path));
        Assert.assertEquals(new String(baos.toByteArray()), new BufferedReader(rd).readLine());
        
        // Add the same file again
        
        fhres = cntmgr.addIpfsContent(addrBob, path);
        Assert.assertTrue(fhres.isAvailable());
        Assert.assertTrue(fhres.isEncrypted());
		Assert.assertEquals(cid, fhres.getCidPath());
    }
}
