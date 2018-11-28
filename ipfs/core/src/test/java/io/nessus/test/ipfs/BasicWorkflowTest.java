package io.nessus.test.ipfs;

import java.io.BufferedReader;

/*-
 * #%L
 * Nessus :: IPFS
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

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Wallet.Address;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.ContentManager.Config;

public class BasicWorkflowTest extends AbstractWorkflowTest {

    long timeout = 2000L;
    int attempts = 5;

    @Test
    public void basicWorkflow() throws Exception {
        
        createContentManager(new Config(blockchain, ipfsClient)
                .ipfsTimeout(timeout)
                .ipfsAttempts(attempts));
        
        // Register the public encryption keys
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        Address addrMary = wallet.getAddress(LABEL_MARY);
        
        PublicKey pubKeyBob = cntmgr.registerAddress(addrBob);
        assertKeyEquals(pubKeyBob, cntmgr.findAddressRegistation(addrBob));
        
        PublicKey pubKeyMary = cntmgr.registerAddress(addrMary);
        assertKeyEquals(pubKeyMary, cntmgr.findAddressRegistation(addrMary));
        
        // Add content to IPFS
        
        Path relPath = Paths.get("bob/userfile.txt");
        InputStream input = getClass().getResourceAsStream("/markdown/etc/userfile.txt");
        FHandle fhandle = cntmgr.addIPFSContent(addrBob, input, relPath);
        Assert.assertTrue(new File(fhandle.getURL().toURI()).exists());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrBob, fhandle.getOwner());
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getCid());
        Assert.assertNotNull(fhandle.getTxId());
        
        // Verify local file content
        
        List<FHandle> fhandles = cntmgr.findLocalContent(addrBob);
        Assert.assertEquals(1, fhandles.size());
        FHandle fhLocal = fhandles.get(0);
        Assert.assertEquals(relPath, fhLocal.getPath());
        Assert.assertTrue(fhLocal.isAvailable());
        Assert.assertFalse(fhLocal.isExpired());
        Assert.assertFalse(fhLocal.isEncrypted());
        
        InputStream reader = cntmgr.getLocalContent(addrBob, relPath);
        BufferedReader br = new BufferedReader(new InputStreamReader(reader));
        Assert.assertEquals("The quick brown fox jumps over the lazy dog.", br.readLine());
        
        Assert.assertTrue(cntmgr.removeLocalContent(addrBob, relPath));
        Assert.assertTrue(cntmgr.findLocalContent(addrBob).isEmpty());
        
        // Find IPFS content on blockchain
        
        String cidBob = fhandle.getCid();
        fhandle = findIPFSContent(addrBob, cidBob, null);
        Assert.assertTrue(fhandle.isAvailable());
        Assert.assertFalse(fhandle.isExpired());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrBob, fhandle.getOwner());
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getTxId());
        
        // Get content from IPFS
        
        fhandle  = cntmgr.getIPFSContent(addrBob, cidBob, relPath, null);
        Assert.assertTrue(new File(fhandle.getURL().toURI()).exists());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrBob, fhandle.getOwner());
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNull(fhandle.getCid());
        
        // Send content to IPFS
        
        fhandle  = cntmgr.sendIPFSContent(addrBob, cidBob, addrMary, null);
        Assert.assertTrue(new File(fhandle.getURL().toURI()).exists());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrMary, fhandle.getOwner());
        Assert.assertNotNull(fhandle.getCid());
        
        // Find IPFS content on blockchain
        
        String cidMary = fhandle.getCid();
        fhandle = findIPFSContent(addrMary, cidMary, null);
        Assert.assertTrue(fhandle.isAvailable());
        Assert.assertFalse(fhandle.isExpired());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrMary, fhandle.getOwner());
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getTxId());
        
        // Get content from IPFS
        
        relPath = Paths.get("marry/userfile.txt");
        fhandle  = cntmgr.getIPFSContent(addrMary, fhandle.getCid(), relPath, null);
        Assert.assertTrue(new File(fhandle.getURL().toURI()).exists());
        Assert.assertEquals(relPath, fhandle.getPath());
        Assert.assertEquals(addrMary, fhandle.getOwner());
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNull(fhandle.getCid());
    }

    private void assertKeyEquals(PublicKey exp, PublicKey was) {
        String encExp = Base64.getEncoder().encodeToString(exp.getEncoded());
        String encWas = Base64.getEncoder().encodeToString(was.getEncoded());
        Assert.assertEquals(encExp, encWas);
    }
    
    @SuppressWarnings("serial")
    static class KnownPubKey implements PublicKey {

        final String encodedKey;
        
        KnownPubKey(String encodedKey) {
            this.encodedKey = encodedKey;
        }

        @Override
        public String getFormat() {
            return "X.509";
        }

        @Override
        public byte[] getEncoded() {
            return Base64.getDecoder().decode(encodedKey);
        }

        @Override
        public String getAlgorithm() {
            return "EC";
        }
    }
}
