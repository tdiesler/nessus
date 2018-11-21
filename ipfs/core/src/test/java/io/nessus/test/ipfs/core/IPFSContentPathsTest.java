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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.nessus.Wallet.Address;
import io.nessus.core.ipfs.FHandle;
import io.nessus.core.ipfs.impl.DefaultContentManager.FHeader;

public class IPFSContentPathsTest extends AbstractWorkflowTest {

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
    public void testPaths() throws Exception {

        String content = "some text";
        InputStream input = new ByteArrayInputStream(content.getBytes());
        
        try {
            cntmgr.add(addrBob, input, null);
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            // ignore
        }
        
        try {
            cntmgr.add(addrBob, input, Paths.get(""));
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            // ignore
        }
        
        try {
            cntmgr.add(addrBob, input, Paths.get(" "));
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            // ignore
        }
        
        // Investigate ipfs paths with whitespace
        // https://github.com/tdiesler/nessus/issues/47
        
        Path srcPath = Paths.get("some space");
        FHandle fhandle = cntmgr.add(addrBob, input, srcPath);
        Path cryptPath = Paths.get(fhandle.getURL().toURI());
        try (Reader rd = new FileReader(cryptPath.toFile())) {
            FHeader fheader = cntmgr.readFHeader(rd);
            Assert.assertEquals(srcPath, fheader.path);
        }
        Path destPath = Paths.get("some other");
        fhandle = cntmgr.decrypt(fhandle, addrBob, destPath, true);
        Path plainPath = Paths.get(fhandle.getURL().toURI());
        Assert.assertTrue(plainPath.toFile().isFile());
        Assert.assertEquals(destPath, plainPath.getFileName());
        try (Reader rd = new FileReader(plainPath.toFile())) {
            String line = new BufferedReader(rd).readLine();
            Assert.assertEquals(content, line);
        }
    }
}

