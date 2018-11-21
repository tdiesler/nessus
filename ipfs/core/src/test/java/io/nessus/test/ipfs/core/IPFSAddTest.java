package io.nessus.test.ipfs.core;

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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.nessus.Wallet.Address;
import io.nessus.core.ipfs.FHandle;

public class IPFSAddTest extends AbstractWorkflowTest {

    @Before
    public void before() throws Exception {
        super.before();
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        PublicKey pubKey = cntmgr.findAddressRegistation(addrBob);
        
        if (pubKey == null) {
            cntmgr.registerAddress(addrBob);
            pubKey = cntmgr.findAddressRegistation(addrBob);
        }
    }
    
    @Test
    public void findNonExisting() throws Exception {

        String content = "some text";
        InputStream input = new ByteArrayInputStream(content.getBytes());
        
        Path path = Paths.get("my/src/path");
        FHandle fhandle = cntmgr.add(addrBob, input, path);
        
        // Unnecessary delay when adding encrypted content
        // https://github.com/tdiesler/nessus/issues/41
        
        // Use an extremely short timeout 
        FHandle fhres = findIPFSContent(addrBob, fhandle.getCid(), 10L);
        Assert.assertTrue(fhres.isAvailable());
    }
}
