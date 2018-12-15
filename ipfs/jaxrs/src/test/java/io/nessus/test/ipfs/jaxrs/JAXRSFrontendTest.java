package io.nessus.test.ipfs.jaxrs;

/*-
 * #%L
 * Nessus :: IPFS :: JAXRS
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.nessus.Wallet.Address;
import io.nessus.ipfs.jaxrs.SAHandle;
import io.nessus.ipfs.jaxrs.JaxrsApplication;
import io.nessus.ipfs.jaxrs.JaxrsApplication.JAXRSServer;
import io.nessus.ipfs.jaxrs.JaxrsClient;
import io.nessus.ipfs.jaxrs.JaxrsConfig;
import io.nessus.ipfs.jaxrs.SFHandle;
import io.nessus.utils.FileUtils;

public class JAXRSFrontendTest extends AbstractJAXRSTest {

    static JAXRSServer server;
    static JaxrsClient client;

    @BeforeClass
    public static void beforeClass() throws Exception {

        AbstractJAXRSTest.beforeClass();

        JaxrsConfig config = new JaxrsConfig.Builder().bcport(18443).build();
        server = JaxrsApplication.serverStart(config);

        URL jaxrsUrl = config.getJaxrsUrl();
        client = new JaxrsClient(jaxrsUrl);
    }

    @AfterClass
    public static void stop() throws Exception {
        if (server != null)
            server.stop();
    }

    @Before
    public void before() {

        // Verify that Bob has some funds
        BigDecimal balBob = wallet.getBalance(addrBob);
        if (balBob.doubleValue() < 1.0)
            wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("1.0"));

        // Verify that Mary has some funds
        BigDecimal balMary = wallet.getBalance(addrMary);
        if (balMary.doubleValue() < 1.0)
            wallet.sendToAddress(addrMary.getAddress(), new BigDecimal("1.0"));
    }

    @After
    public void after() {

        redeemLockedUtxos(addrBob);
        redeemLockedUtxos(addrMary);
    }

    @Test
    public void basicWorkflow() throws Exception {

        Long timeout = 4000L;

        // Register Bob's public encryption key

        SAHandle ahBob = client.registerAddress(addrBob.getAddress());
        Assert.assertNotNull(ahBob.getEncKey());

        // Find Bob's pubKey registration

        List<SAHandle> addrs = client.findAddressInfo(null, addrBob.getAddress());
        Assert.assertEquals(1, addrs.size());
        Assert.assertEquals(ahBob.getEncKey(), addrs.get(0).getEncKey());

        // Register Mary's public encryption key

        SAHandle ahMary = client.registerAddress(addrMary.getAddress());
        Assert.assertNotNull(ahMary.getEncKey());

        // Find Mary's pubKey registration

        addrs = client.findAddressInfo(null, addrMary.getAddress());
        Assert.assertEquals(ahMary.getEncKey(), addrs.get(0).getEncKey());

        // Add content to IPFS

        Path relPath = Paths.get("Bob/userfile.txt");
        FileUtils.recursiveDelete(cntmgr.getPlainPath(addrBob).resolve("Bob"));
        InputStream input = getClass().getResourceAsStream("/userfile.txt");

        SFHandle fhandle = client.addIpfsContent(addrBob.getAddress(), relPath.toString(), input);

        Assert.assertEquals(addrBob, wallet.findAddress(fhandle.getOwner()));
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getCid());

        // Verify local file content

        SFHandle fhLocal = findLocalContent(addrBob, relPath);
        Assert.assertEquals(relPath.toString(), fhLocal.getPath());
        Assert.assertTrue(fhLocal.isAvailable());
        Assert.assertFalse(fhLocal.isExpired());
        Assert.assertFalse(fhLocal.isEncrypted());

        input = client.getLocalContent(addrBob.getAddress(), relPath.toString());
        BufferedReader br = new BufferedReader(new InputStreamReader(input));
        Assert.assertEquals("The quick brown fox jumps over the lazy dog.", br.readLine());

        Assert.assertTrue(client.removeLocalContent(addrBob.getAddress(), relPath.toString()));
        Assert.assertNull(findLocalContent(addrBob, relPath));

        // Find IPFS content on blockchain

        String cidBob = fhandle.getCid();
        fhandle = findIPFSContent(addrBob, cidBob, timeout);
        Assert.assertTrue(fhandle.isAvailable());
        Assert.assertFalse(fhandle.isExpired());
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertEquals(addrBob, wallet.findAddress(fhandle.getOwner()));
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getTxId());

        // Get content from IPFS

        String cid = fhandle.getCid();

        fhandle = client.getIpfsContent(addrBob.getAddress(), cid, relPath.toString(), timeout);

        Assert.assertEquals(addrBob, wallet.findAddress(fhandle.getOwner()));
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNull(fhandle.getCid());

        // Send content from IPFS

        fhandle = client.sendIpfsContent(addrBob.getAddress(), cid, addrMary.getAddress(), timeout);

        Assert.assertEquals(addrMary, wallet.findAddress(fhandle.getOwner()));
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getCid());

        // Find IPFS content on blockchain

        String cidMary = fhandle.getCid();
        fhandle = findIPFSContent(addrMary, cidMary, timeout);
        Assert.assertTrue(fhandle.isAvailable());
        Assert.assertFalse(fhandle.isExpired());
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertEquals(addrMary, wallet.findAddress(fhandle.getOwner()));
        Assert.assertTrue(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getTxId());

        // Get content from IPFS

        relPath = Paths.get("marry/userfile.txt");
        FileUtils.recursiveDelete(cntmgr.getPlainPath(addrMary));
        fhandle = client.getIpfsContent(addrMary.getAddress(), fhandle.getCid(), relPath.toString(), timeout);

        Assert.assertEquals(addrMary, wallet.findAddress(fhandle.getOwner()));
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNull(fhandle.getCid());
        
        input = client.getLocalContent(addrMary.getAddress(), relPath.toString());
        br = new BufferedReader(new InputStreamReader(input));
        Assert.assertEquals("The quick brown fox jumps over the lazy dog.", br.readLine());
        
        // Find local content
        
        List<SFHandle> fhandles = client.findLocalContent(addrMary.getAddress(), null);
        Assert.assertEquals(1, fhandles.size());
        SFHandle fhParent = fhandles.get(0);
        Assert.assertEquals(1, fhParent.getChildren().size());
        SFHandle fhChild = fhParent.getChildren().get(0);
        Assert.assertEquals(relPath, Paths.get(fhChild.getPath()));
        
        fhandles = client.findLocalContent(addrMary.getAddress(), relPath.toString());
        Assert.assertEquals(1, fhandles.size());
        Assert.assertEquals(0, fhandles.get(0).getChildren().size());
        Assert.assertEquals(relPath, Paths.get(fhandles.get(0).getPath()));
    }

    private SFHandle findLocalContent(Address addr, Path path) throws IOException {
        List<SFHandle> fhandles = client.findLocalContent(addr.getAddress(), path.toString());
        SFHandle fhLocal = fhandles.stream().findFirst().orElse(null);
        return fhLocal;
    }

    private SFHandle findIPFSContent(Address addr, String cid, Long timeout) throws IOException, InterruptedException {

        List<SFHandle> fhandles = client.findIpfsContent(addr.getAddress(), timeout);
        SFHandle fhandle  = fhandles.stream().filter(fh -> fh.getCid().equals(cid)).findFirst().get();
        Assert.assertNotNull(fhandle);
        Assert.assertTrue(fhandle.isAvailable());

        return fhandle;
    }
}
