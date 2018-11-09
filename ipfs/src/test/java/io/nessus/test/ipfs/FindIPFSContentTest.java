package io.nessus.test.ipfs;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Wallet.Address;
import io.nessus.ipfs.FHandle;

public class FindIPFSContentTest extends AbstractWorkflowTest {

    @Test
    public void basicWorkflow() throws Exception {

        /*
            $ ipfs add -r ipfs/src/test/resources/contentA
            added QmQrDwzQzvBSJaAyz9VFWKB8vjhYtjTUWBEBmkYuGtjQW3 contentA/file01.txt
            added QmbEsRMKVUSjUYenh4gvw4UcqUGRGTGC7D221eU4ffpxLa contentA/file02.txt
            added QmctTBqcHf1A3uQhrTZgSqzV8Yh7nD9ud1tkPF58DPJFbw contentA/file03.txt
            added QmRdRPCf9dWp5NMgkK8ngh6sg7BppZAfdniJCN4jLsVXqS contentA
            
            $ ipfs add -r ipfs/src/test/resources/contentB
            added QmRyMvHP7a78rvWZ7RqBTEFfz26dc61HWc4rPtEsn2h774 contentB/file04.txt
            added Qmb7nmz3nqrJMQsbsqeJUz9VXRMbYv95HF5hEqVU1XjGRS contentB/file05.txt
            added QmScDtKeBGD7vNKdCFKTvmgq6tK12wD1aAxd8HrrXdquvk contentB/file06.txt
            added Qmf4SQhCBJNCiMhqoDNu6pW39Fh9pVonsGQZH25WibQ6tC contentB
        */
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        cntmgr.register(addrBob);
        
        addContent(addrBob, "contentA/file01.txt");
        addContent(addrBob, "contentA/file02.txt");
        addContent(addrBob, "contentA/file03.txt");
        
        List<FHandle> fhandles = cntmgr.findIPFSContent(addrBob, null);
        fhandles.forEach(fh -> LOG.info("{}", fh));
        Assert.assertEquals(3, fhandles.size());
    }
}
