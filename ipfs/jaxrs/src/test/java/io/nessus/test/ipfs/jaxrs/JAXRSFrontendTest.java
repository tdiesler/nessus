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
import java.net.URI;
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
import io.nessus.ipfs.jaxrs.JAXRSApplication;
import io.nessus.ipfs.jaxrs.JAXRSApplication.JAXRSServer;
import io.nessus.ipfs.jaxrs.JAXRSClient;
import io.nessus.ipfs.jaxrs.SFHandle;

public class JAXRSFrontendTest extends AbstractJAXRSTest {

    static JAXRSServer server;
    static JAXRSClient client;

    @BeforeClass
    public static void beforeClass() throws Exception {

        AbstractJAXRSTest.beforeClass();

        server = JAXRSApplication.serverStart();

        int port = server.getJAXRSConfig().getPort();
        String host = server.getJAXRSConfig().getHost();
        client = new JAXRSClient(new URI(String.format("http://%s:%d/nessus", host, port)));
    }

    @AfterClass
    public static void stop() throws Exception {
        if (server != null)
            server.stop();
    }

    @Before
    public void before() {

        redeemLockedUtxos(LABEL_BOB, addrBob);
        redeemLockedUtxos(LABEL_MARY, addrMary);

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

        redeemLockedUtxos(LABEL_BOB, addrBob);
        redeemLockedUtxos(LABEL_MARY, addrMary);
    }

    @Test
    public void basicWorkflow() throws Exception {

        Long timeout = 4000L;

        // Register Bob's public encryption key

        String encKey = client.registerAddress(addrBob.getAddress());
        Assert.assertNotNull(encKey);

        // Find Bob's pubKey registration

        String wasKey = client.findAddressRegistation(addrBob.getAddress());
        Assert.assertEquals(encKey, wasKey);

        // Register Mary's public encryption key

        encKey = client.registerAddress(addrMary.getAddress());
        Assert.assertNotNull(encKey);

        // Find Mary's pubKey registration

        wasKey = client.findAddressRegistation(addrMary.getAddress());
        Assert.assertEquals(encKey, wasKey);

        // Add content to IPFS

        Path relPath = Paths.get("bob/userfile.txt");
        InputStream input = getClass().getResourceAsStream("/userfile.txt");

        SFHandle fhandle = client.add(addrBob.getAddress(), relPath.toString(), input);

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

        InputStream reader = client.getLocalContent(addrBob.getAddress(), relPath.toString());
        BufferedReader br = new BufferedReader(new InputStreamReader(reader));
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

        fhandle = client.get(addrBob.getAddress(), cid, relPath.toString(), timeout);

        Assert.assertEquals(addrBob, wallet.findAddress(fhandle.getOwner()));
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNull(fhandle.getCid());

        // Send content from IPFS

        fhandle = client.send(addrBob.getAddress(), cid, addrMary.getAddress(), timeout);

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
        fhandle = client.get(addrMary.getAddress(), fhandle.getCid(), relPath.toString(), timeout);

        Assert.assertEquals(addrMary, wallet.findAddress(fhandle.getOwner()));
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNull(fhandle.getCid());
    }

    private SFHandle findLocalContent(Address addr, Path path) throws IOException {
        SFHandle fhLocal = client.findLocalContent(addr.getAddress()).stream()
                .filter(fh -> Paths.get(fh.getPath()).equals(path))
                .findFirst().orElse(null);
        return fhLocal;
    }

    private SFHandle findIPFSContent(Address addr, String cid, Long timeout) throws IOException, InterruptedException {

        List<SFHandle> fhandles = client.findIPFSContent(addr.getAddress(), timeout);
        SFHandle fhandle  = fhandles.stream().filter(fh -> fh.getCid().equals(cid)).findFirst().get();
        Assert.assertNotNull(fhandle);
        Assert.assertTrue(fhandle.isAvailable());

        return fhandle;
    }
}
