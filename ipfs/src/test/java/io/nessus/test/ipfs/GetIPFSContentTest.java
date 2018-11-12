package io.nessus.test.ipfs;

import java.nio.file.Path;
import java.security.PublicKey;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Wallet.Address;
import io.nessus.ipfs.FHandle;

public class GetIPFSContentTest extends AbstractWorkflowTest {

    long timeout = 2000;
    int attempts = 5;
    
    @Test
    public void findNonExisting() throws Exception {

        createContentManager(timeout, attempts);
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        PublicKey pubKey = cntmgr.findRegistation(addrBob);
        
        if (pubKey == null) {
            cntmgr.register(addrBob);
            pubKey = cntmgr.findRegistation(addrBob);
        }
        Assert.assertNotNull(pubKey);
        
        Path path01 = getTestPath(01);
        
        List<FHandle> fhandles = cntmgr.findIPFSContent(addrBob, null);
        fhandles.forEach(fh -> LOG.info("{}", fh));
        
        FHandle fh01 = fhandles.stream().filter(fh -> path01.equals(fh.getPath())).findFirst().orElse(null);
        
        if (fh01 == null) {
            long millis = System.currentTimeMillis();
            String cid01 = addContent(addrBob, path01, "test01_" + millis).getCid();
            fh01 = ipfsGet(addrBob, cid01);
        }
        Assert.assertTrue(fh01.isAvailable());
        
        fhandles = cntmgr.findIPFSContent(addrBob, null);
        fhandles = awaitFileAvailability(fhandles, 1, true);
        fh01 = fhandles.stream().filter(fh -> path01.equals(fh.getPath())).findFirst().orElse(null);
        Assert.assertTrue(fh01.isAvailable());
        
        // SET A BREAKPOINT HERE AND CONTINUE WITH A NEW IPFS INSTANCE
        
        createContentManager(timeout, attempts);
        
        fhandles = cntmgr.findIPFSContent(addrBob, null);
        fhandles.forEach(fh -> LOG.info("{}", fh));
        
        fh01 = fhandles.stream().filter(fh -> path01.equals(fh.getPath())).findFirst().orElse(null);
        
        if (fh01 == null) {
            long millis = System.currentTimeMillis();
            String cid01 = addContent(addrBob, path01, "test01_" + millis).getCid();
            fh01 = ipfsGet(addrBob, cid01);
        }
        Assert.assertTrue(fh01.isAvailable());
        
        fhandles = cntmgr.findIPFSContent(addrBob, null);
        fhandles = awaitFileAvailability(fhandles, 1, true);
        fh01 = fhandles.stream().filter(fh -> path01.equals(fh.getPath())).findFirst().orElse(null);
        Assert.assertTrue(fh01.isAvailable());
    }
}
