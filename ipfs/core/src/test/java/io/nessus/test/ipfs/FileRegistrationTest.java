package io.nessus.test.ipfs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.ipfs.multihash.Multihash;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.core.AHandle;
import io.nessus.utils.FileUtils;

public class FileRegistrationTest extends AbstractIpfsTest {

    @Test
    public void findFileReg() throws Exception {

    	// Unlock & send all to the sink
    	unlockFileRegistrations(addrBob);
    	wallet.sendFromAddress(addrBob, addrSink.getAddress(), Wallet.ALL_FUNDS);
    	
    	// Remove all of Bob's local content
    	cntmgr.removeLocalContent(addrBob, null);
    	
        // Send 1 BTC to Bob
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("1.0"));
        
        // Register Bob's public encryption key
        
        cntmgr.registerAddress(addrBob);
        
        // Add some IPFS content
        
        InputStream input = new ByteArrayInputStream("Hello Kermit".getBytes());
        FHandle fhA = cntmgr.addIpfsContent(addrBob, Paths.get("kermit.txt"), input);
        
        input = new ByteArrayInputStream("Hello Piggy".getBytes());
        FHandle fhB = cntmgr.addIpfsContent(addrBob, Paths.get("piggy.txt"), input);
        
        List<FHandle> fhandles = cntmgr.findIpfsContent(addrBob, null);
        Assert.assertEquals(2, fhandles.size());
        Assert.assertTrue(fhandles.contains(fhA));
        Assert.assertTrue(fhandles.contains(fhB));
    }

    @Test
    public void findMissingFileReg() throws Exception {

        // Create a new address for Lui
        Address addrLui = wallet.newAddress("Lui");

        AHandle ahA = cntmgr.findAddressRegistation(addrLui, null);
        Assert.assertNull(ahA);
        
        // Send 1 BTC to Lui
    	
        wallet.sendToAddress(addrLui.getAddress(), new BigDecimal("1.0"));
        
        // Register an address without IPFS add
        
        AHandle ahB = cntmgr.registerAddress(addrLui);
        Assert.assertNotNull(ahB.getPubKey());
        Assert.assertTrue(ahB.isAvailable());
        Assert.assertNotNull(ahB.getCid());
        
        // Register IPFS content without adding the file 
        
        InputStream input = new ByteArrayInputStream("Hello Kermit".getBytes());
        FHandle fhA = cntmgr.addIpfsContent(addrLui, Paths.get("kermit.txt"), input, true);
        Assert.assertTrue(fhA.isAvailable());
        Assert.assertTrue(fhA.isEncrypted());
        Assert.assertNotNull(fhA.getCid());
        
        // Clear the IPFS cache & move crypt path content
        
        cntmgr.getIPFSCache().clear();
        
        Multihash cid = fhA.getCid();
		Path cryptPath = cntmgr.getCryptPath(addrLui);
        Path fullPath = cryptPath.resolve(cid.toBase58());
        Path tmpPath = cntmgr.getTempPath().resolve(cid.toBase58());
        Files.move(fullPath, tmpPath, StandardCopyOption.ATOMIC_MOVE);

        // Verify that we cannot find the file
        
        List<FHandle> fhandles = cntmgr.findIpfsContent(addrLui, 3000L);
        Assert.assertEquals(1, fhandles.size());
		Assert.assertFalse(fhandles.get(0).isAvailable());

        // Add the content to IPFS
		
		List<Multihash> cids = ipfsClient.add(tmpPath);
        Assert.assertEquals(1, cids.size());
        Assert.assertEquals(fhA.getCid(), cids.get(0));
        FileUtils.recursiveDelete(tmpPath);
        
        // Verify that we now can find the file
        
        fhandles = cntmgr.findIpfsContent(addrLui, 3000L);
        Assert.assertEquals(1, fhandles.size());
        FHandle fhC = fhandles.get(0);
		Assert.assertTrue(fhC.isAvailable());
        Assert.assertTrue(fhC.isEncrypted());
        Assert.assertEquals(fhA.getCid(), fhC.getCid());
        
        cntmgr.unregisterIpfsContent(addrLui, null);
    	cntmgr.unregisterAddress(addrLui);
    }

    @Test
    public void unregisterFile() throws Exception {

    	// Unlock & send all to the sink
    	unlockFileRegistrations(addrBob);
    	wallet.sendFromAddress(addrBob, addrSink.getAddress(), Wallet.ALL_FUNDS);
    	
    	// Remove all of Bob's local content
    	cntmgr.removeLocalContent(addrBob, null);
    	
        // Send 1 BTC to Bob
        wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("1.0"));
        
        InputStream input = new ByteArrayInputStream("Hello Kermit".getBytes());
        FHandle fhA = cntmgr.addIpfsContent(addrBob, Paths.get("kermit.txt"), input);
        
        input = new ByteArrayInputStream("Hello Piggy".getBytes());
        FHandle fhB = cntmgr.addIpfsContent(addrBob, Paths.get("piggy.txt"), input);
        
        List<FHandle> fhandles = cntmgr.findIpfsContent(addrBob, null);
        Assert.assertEquals(2, fhandles.size());
        
        List<Multihash> cids = Arrays.asList(fhA.getCid(), fhB.getCid());
        List<Multihash> cres = cntmgr.unregisterIpfsContent(addrBob, cids);
        Assert.assertEquals(2, cres.size());
        
        fhandles = cntmgr.findIpfsContent(addrBob, null);
        Assert.assertEquals(0, fhandles.size());
    }
}
