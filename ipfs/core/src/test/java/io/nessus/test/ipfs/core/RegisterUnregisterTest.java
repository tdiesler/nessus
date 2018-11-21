package io.nessus.test.ipfs.core;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;

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

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.Wallet.Address;
import io.nessus.core.ipfs.FHandle;

public class RegisterUnregisterTest extends AbstractWorkflowTest {

    @Test
    public void unregisterAddress() throws Exception {

        Address addrBob = wallet.getAddress(LABEL_BOB);
        PublicKey pubKey = cntmgr.findAddressRegistation(addrBob);
        
        if (pubKey == null) {
            cntmgr.registerAddress(addrBob);
            pubKey = cntmgr.findAddressRegistation(addrBob);
        }
        
        Assert.assertNotNull(pubKey);
        
        PublicKey resKey = cntmgr.unregisterAddress(addrBob);
        Assert.assertEquals(pubKey, resKey);
        
        pubKey = cntmgr.findAddressRegistation(addrBob);
        Assert.assertNull(pubKey);
    }
    
    @Test
    public void unregisterIPFS() throws Exception {

        Address addrBob = wallet.getAddress(LABEL_BOB);
        PublicKey pubKey = cntmgr.findAddressRegistation(addrBob);
        
        if (pubKey == null) {
            cntmgr.registerAddress(addrBob);
            pubKey = cntmgr.findAddressRegistation(addrBob);
        }
        
        ByteArrayInputStream input = new ByteArrayInputStream("Hello Kermit".getBytes());
        FHandle fhandle = cntmgr.add(addrBob, input, Paths.get("some.txt"));
        
        List<FHandle> fhandles = cntmgr.findIPFSContent(addrBob, null);
        Assert.assertEquals(1, fhandles.size());
        Assert.assertEquals(fhandle, fhandles.get(0));
        
        List<FHandle> fhress = cntmgr.unregisterIPFSContent(addrBob, Arrays.asList(fhandle.getCid()));
        Assert.assertEquals(1, fhress.size());
        Assert.assertEquals(fhandle, fhress.get(0));
        
        fhandles = cntmgr.findIPFSContent(addrBob, null);
        Assert.assertEquals(0, fhandles.size());
    }
}
