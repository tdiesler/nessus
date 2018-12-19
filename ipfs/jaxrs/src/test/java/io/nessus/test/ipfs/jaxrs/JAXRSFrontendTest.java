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
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.nessus.Wallet.Address;
import io.nessus.ipfs.jaxrs.JAXRSApplication;
import io.nessus.ipfs.jaxrs.JAXRSApplication.JAXRSServer;
import io.nessus.ipfs.jaxrs.JAXRSClient;
import io.nessus.ipfs.jaxrs.JAXRSConfig;
import io.nessus.ipfs.jaxrs.JAXRSConfig.JAXRSConfigBuilder;
import io.nessus.ipfs.jaxrs.SAHandle;
import io.nessus.ipfs.jaxrs.SFHandle;
import io.nessus.utils.FileUtils;

public class JAXRSFrontendTest extends AbstractJAXRSTest {

    static JAXRSConfig config;
    static JAXRSServer server;
    static JAXRSClient client;

    @Before
    public void before() throws Exception {
    	super.before();

    	if (server == null) {
    		
            config = new JAXRSConfigBuilder().bcport(18443).build();
            server = JAXRSApplication.serverStart(config);

            URL jaxrsUrl = config.getJaxrsUrl();
            client = new JAXRSClient(jaxrsUrl);
    	}
    }

    @AfterClass
    public static void stop() throws Exception {
        if (server != null)
            server.stop();
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
        FileUtils.recursiveDelete(getPlainPath(addrBob).resolve("Bob"));
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
        Assert.assertNotNull(fhandle.getCid());

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

        relPath = Paths.get("mary/userfile.txt");
        FileUtils.recursiveDelete(getPlainPath(addrMary));
        fhandle = client.getIpfsContent(addrMary.getAddress(), fhandle.getCid(), relPath.toString(), timeout);

        Assert.assertEquals(addrMary, wallet.findAddress(fhandle.getOwner()));
        Assert.assertEquals(relPath, Paths.get(fhandle.getPath()));
        Assert.assertFalse(fhandle.isEncrypted());
        Assert.assertNotNull(fhandle.getCid());
        
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

    @Test
    public void fileTree() throws Exception {

        Path srcPath = Paths.get("src/test/resources/contentA");
        Path dstPath = getPlainPath(addrBob).resolve("contentA");
        if (!dstPath.toFile().exists())
            FileUtils.recursiveCopy(srcPath, dstPath);
        
        client.findLocalContent(addrBob.getAddress(), "contentA");
        List<SFHandle> sfhs = client.findLocalContent(addrBob.getAddress(), "contentA");
        Assert.assertEquals(1, sfhs.size());
        
        SFHandle sfh = sfhs.get(0);
		LOG.info("{}", sfh.toString(true));

        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer();
        writer = mapper.writerWithDefaultPrettyPrinter();
        String strres = writer.writeValueAsString(sfh);
        LOG.info("{}", strres);
        Assert.assertFalse(strres.contains(": null"));
    }
    

    @Test
    public void invalidRootPath() throws Exception {

    	SAHandle shandle = client.registerAddress(addrBob.getAddress());
		LOG.info("{}", shandle);
		
        List<SAHandle> infos = client.findAddressInfo(null, addrBob.getAddress());
        infos.forEach(info -> LOG.info("{}", info));
		
        infos = client.findAddressInfo(LABEL_BOB, null);
        infos.forEach(info -> LOG.info("{}", info));
		
		URL jaxrsUrl = config.getJaxrsUrl();
		URL bogusUrl = new URL(jaxrsUrl.toString().replace("nessus", "bogus"));
		JAXRSClient bogusClient = new JAXRSClient(bogusUrl);
        
        try {
			bogusClient.findAddressInfo(null, addrBob.getAddress());
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException rte) {
			// ignore
		}
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
