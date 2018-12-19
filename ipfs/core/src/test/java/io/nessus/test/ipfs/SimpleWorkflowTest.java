package io.nessus.test.ipfs;

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.ipfs.multihash.Multihash;
import io.nessus.ipfs.ContentManagerConfig;
import io.nessus.ipfs.ContentManagerConfig.ContentManagerConfigBuilder;
import io.nessus.ipfs.FHandle;

public class SimpleWorkflowTest extends AbstractIpfsTest {

    long timeout = 2000L;
    int attempts = 5;

    @Test
    public void basicWorkflow() throws Exception {
        
		ContentManagerConfig config = new ContentManagerConfigBuilder()
        		.bcurl(DEFAULT_JSONRPC_REGTEST_URL)
                .ipfsAttempts(attempts)
                .ipfsTimeout(timeout)
        		.build();
        
        createContentManager(config);
        
        // Register the public encryption keys
        
        PublicKey pubKeyBob = cntmgr.registerAddress(addrBob).getPubKey();
        assertKeyEquals(pubKeyBob, cntmgr.findAddressRegistation(addrBob, null).getPubKey());
        
        PublicKey pubKeyMary = cntmgr.registerAddress(addrMary).getPubKey();
        assertKeyEquals(pubKeyMary, cntmgr.findAddressRegistation(addrMary, null).getPubKey());
        
        // Add content to IPFS
        
        Path relPath = Paths.get("bob/userfile.txt");
        InputStream input = getClass().getResourceAsStream("/markdown/etc/userfile.txt");
        FHandle fhandle = cntmgr.addIpfsContent(addrBob, relPath, input);
        Assert.assertTrue(fhandle.getFilePath().toFile().exists());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrBob, fhandle.getOwner());
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getCid());
        Assert.assertNotNull(fhandle.getTxId());
        
        // Verify local file content
        
        List<FHandle> fhandles = cntmgr.findLocalContent(addrBob);
        Assert.assertEquals(1, fhandles.size());
        FHandle fhParent = fhandles.get(0);
        Assert.assertEquals(1, fhParent.getChildren().size());
        FHandle fhChild = fhParent.getChildren().get(0);
        Assert.assertEquals(relPath, fhChild.getPath());
        Assert.assertTrue(fhChild.isAvailable());
        Assert.assertFalse(fhChild.isExpired());
        Assert.assertFalse(fhChild.isEncrypted());
        
        InputStream reader = cntmgr.getLocalContent(addrBob, relPath);
        BufferedReader br = new BufferedReader(new InputStreamReader(reader));
        Assert.assertEquals("The quick brown fox jumps over the lazy dog.", br.readLine());
        
        Assert.assertTrue(cntmgr.removeLocalContent(addrBob, relPath));
        Assert.assertTrue(cntmgr.findLocalContent(addrBob).isEmpty());
        
        // Find IPFS content on blockchain
        
        Multihash cidBob = fhandle.getCid();
        fhandle = findIpfsContent(addrBob, cidBob, null);
        Assert.assertTrue(fhandle.isAvailable());
        Assert.assertFalse(fhandle.isExpired());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrBob, fhandle.getOwner());
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getTxId());
        
        // Get content from IPFS
        
        fhandle  = cntmgr.getIpfsContent(addrBob, cidBob, relPath, null);
        Assert.assertTrue(fhandle.getFilePath().toFile().exists());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrBob, fhandle.getOwner());
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getCid());
        
        // Send content to IPFS
        
        fhandle  = cntmgr.sendIpfsContent(addrBob, cidBob, addrMary, null);
        Assert.assertTrue(fhandle.getFilePath().toFile().exists());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrMary, fhandle.getOwner());
        Assert.assertNotNull(fhandle.getCid());
        
        // Find IPFS content on blockchain
        
        Multihash cidMary = fhandle.getCid();
        fhandle = findIpfsContent(addrMary, cidMary, null);
        Assert.assertTrue(fhandle.isAvailable());
        Assert.assertFalse(fhandle.isExpired());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrMary, fhandle.getOwner());
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getTxId());
        
        // Get content from IPFS
        
        relPath = Paths.get("marry/userfile.txt");
        fhandle  = cntmgr.getIpfsContent(addrMary, fhandle.getCid(), relPath, null);
        Assert.assertTrue(fhandle.getFilePath().toFile().exists());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrMary, fhandle.getOwner());
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getCid());
    }

    private void assertKeyEquals(PublicKey exp, PublicKey was) {
        String encExp = Base64.getEncoder().encodeToString(exp.getEncoded());
        String encWas = Base64.getEncoder().encodeToString(was.getEncoded());
        Assert.assertEquals(encExp, encWas);
    }
}
