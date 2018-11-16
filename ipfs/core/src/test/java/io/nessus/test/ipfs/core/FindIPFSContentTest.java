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

import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.core.ipfs.FHandle;
import io.nessus.utils.TimeUtils;

public class FindIPFSContentTest extends AbstractWorkflowTest {

    long timeout = 2000L;
    int attempts = 5;

    @Before
    public void before() throws Exception {
        super.before();
        
        Address addrBob = wallet.getAddress(LABEL_BOB);
        PublicKey pubKey = cntmgr.findRegistation(addrBob);
        
        if (pubKey == null) {
            cntmgr.register(addrBob);
            pubKey = cntmgr.findRegistation(addrBob);
        }
    }
    
    @Test
    public void findTiming() throws Exception {

        createContentManager(timeout, attempts);
        
        Date start = new Date();
        addContent(addrBob, getTestPath(100), "test100_" + start.getTime());
        LOG.info("addContent: {}", TimeUtils.elapsedTimeString(start));

        start = new Date();
        List<FHandle> fhandles = cntmgr.findIPFSContent(addrBob, 20000L);
        LOG.info("findIPFSContent: {}", TimeUtils.elapsedTimeString(start));
        fhandles.forEach(fh -> LOG.info("{}", fh));
    }

    @Test
    public void spendFileRegs() throws Exception {

        createContentManager(timeout, attempts);
        
        List<FHandle> fhandles = cntmgr.findIPFSContent(addrBob, null);
        if (fhandles.isEmpty()) {
            addContent(addrBob, Paths.get("contentA/file01.txt"));
            addContent(addrBob, Paths.get("contentA/file02.txt"));
            addContent(addrBob, Paths.get("contentA/file03.txt"));
        }
        
        fhandles = cntmgr.findIPFSContent(addrBob, null);
        fhandles.forEach(fh -> LOG.info("{}", fh));
        Assert.assertEquals(3, fhandles.size());
        
        awaitFileAvailability(fhandles, 3, true);
        
        // Spend Bob's file registrations
        unlockFileRegistrations(addrBob);
        wallet.sendFromLabel(LABEL_BOB, addrBob.getAddress(), Wallet.ALL_FUNDS);
        
        // Verify that no IPFS files are found
        fhandles = cntmgr.findIPFSContent(addrBob, null);
        Assert.assertTrue(fhandles.isEmpty());
    }

    @Test
    public void findNonExisting() throws Exception {

        createContentManager(timeout, attempts);
        
        List<FHandle> fhandles = cntmgr.findIPFSContent(addrBob, null);
        if (fhandles.isEmpty()) {
            long millis = System.currentTimeMillis();
            addContent(addrBob, getTestPath(200), "test200_" + millis);
            addContent(addrBob, getTestPath(201), "test201_" + millis);
            addContent(addrBob, getTestPath(202), "test202_" + millis);
        }
        
        fhandles = cntmgr.findIPFSContent(addrBob, null);
        fhandles = awaitFileAvailability(fhandles, 3, true);
        
        // SET A BREAKPOINT HERE AND CONTINUE WITH A NEW IPFS INSTANCE
        
        createContentManager(timeout, attempts);
        
        fhandles = cntmgr.findIPFSContent(addrBob, null);
        fhandles.forEach(fh -> LOG.info("{}", fh));
        
        // Verify that no IPFS files are found
        
        fhandles = awaitFileAvailability(fhandles, 1, false);
        if (fhandles.size() == 0) {
            long millis = System.currentTimeMillis();
            addContent(addrBob, getTestPath(200), "test200_" + millis);
            addContent(addrBob, getTestPath(201), "test201_" + millis);
            addContent(addrBob, getTestPath(202), "test202_" + millis);
        }
        
        fhandles = cntmgr.findIPFSContent(addrBob, null);
        fhandles.forEach(fh -> LOG.info("{}", fh));
        
        awaitFileAvailability(fhandles, 3, true);
    }
}

/*

killall ipfs
 
rm -rf ~/.bitcoin/regtest
rm -rf ~/.ipfs; ipfs init; ipfs daemon &
rm -rf ~/.fman

*/
