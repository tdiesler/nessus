package io.nessus.test.ipfs.client;

import static io.nessus.ipfs.client.IPFSClient.DEFAULT_IPFS_ADDR;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.ipfs.multihash.Multihash;
import io.nessus.ipfs.client.DefaultIPFSClient;
import io.nessus.ipfs.client.IPFSClient;
import io.nessus.utils.StreamUtils;

/**
 * Connect to some other IPFS server
 * 
 * ipfs swarm connect /ip4/95.179.155.125/tcp/4001/ipfs/QmdGP46wxmV5eRSzKNQpd88yW7rEWnZNC4Ba5cMGRtHfid
 */
public class IPFSClientTest {

    static IPFSClient client;
    
    @BeforeClass
    public static void beforeClass() throws IOException {
        client = new DefaultIPFSClient(DEFAULT_IPFS_ADDR).connect();
        Path path = Paths.get("src/test/resources/html");
        List<Multihash> cids = client.add(path);
        Assert.assertEquals(10, cids.size());
        Assert.assertEquals("Qme6hd6tYXTFb7bb7L3JZ5U6ygktpAHKxbaeffYyQN85mW", cids.get(9).toBase58());
    }
    
    @Test
    public void version() throws Exception {
        String version = client.version();
        Assert.assertTrue("Start with 0.4.x - " + version, version.startsWith("0.4."));
    }

    @Test
    public void peerId() throws Exception {
        String peerId = client.getPeerId();
        Assert.assertNotNull(peerId);
    }

    @Test
    public void basicOps() throws Exception {
        
    	Multihash HASH = Multihash.fromBase58("QmUD7uG5prAMHbcCfp4x1G1mMSpywcSMHTGpq62sbpDAg6");
        
        // add
        
        URL furl = getClass().getResource("/html/etc/userfile.txt");
        Path path = Paths.get(furl.getPath());
        Multihash resHash = client.addSingle(path);
        Assert.assertEquals(HASH, resHash);

        // cat 
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Future<InputStream> future = client.cat(HASH);
		StreamUtils.copyStream(future.get(), baos);
        Assert.assertEquals("The quick brown fox jumps over the lazy dog.", new String (baos.toByteArray()));

        // get 
        path = client.get(HASH, Paths.get("target/ipfs")).get();
        Assert.assertTrue("Is file: " + path, path.toFile().isFile());
    }

    @Test
    public void binaryAddGet() throws Exception {
        
        Multihash HASH = Multihash.fromBase58("QmaMgvGJjZU511pzH1fSwh9RRnKckyujoRxVeDSEaEGM5N");
        
        // add
        
        Path path = Paths.get("src/test/resources/html/img/logo.png");
        Multihash resHash = client.addSingle(path);
        Assert.assertEquals(HASH, resHash);

        // get 
        path = client.get(HASH, Paths.get("target/ipfs")).get();
        Assert.assertTrue("Is file: " + path, path.toFile().isFile());
    }

    @Test
    public void binaryAddGetInSubDir() throws Exception {
        
        Multihash HASH = Multihash.fromBase58("QmYhaNnLGtFDEc559T9bVkqYqaXLGojMWDzVqjFZgrmnCi");
        
        // add
        
        Path path = Paths.get("src/test/resources/html/img");
        List<Multihash> cids = client.add(path);
        Assert.assertEquals(2, cids.size());
        Assert.assertEquals(HASH, cids.get(1));

        // get 
        path = client.get(HASH, Paths.get("target/ipfs")).get();
        Assert.assertTrue("Is dir: " + path, path.toFile().isDirectory());
        Assert.assertTrue(path.resolve("logo.png").toFile().isFile());
    }

    @Test
    public void getWithTimeout() throws Exception {
        
        /*
        $ ipfs add -r ipfs/core/src/test/resources/html
        added QmUNRu2qVDFoA7hg37E7mNCkBurvyfvjRjUJT8d2LwUkDT html/chap/ch01.html
        added QmT1cVKZQPvKhS8Rg9ek7UgThwFC8gC6gAJBiRwMKMnYyg html/css/default.css
        added QmUD7uG5prAMHbcCfp4x1G1mMSpywcSMHTGpq62sbpDAg6 html/etc/userfile.txt
        added QmaMgvGJjZU511pzH1fSwh9RRnKckyujoRxVeDSEaEGM5N html/img/logo.png
        added QmYgjSRbXFPdPYKqQSnUjmXLYLudVahEJQotMaAJKt6Lbd html/index.html
        added QmSEiybH7aXnsiXN8jY6hGECK5Mzj5nwu2ijHhhCWTzHEs html/chap
        added QmfPRsChuVCsnN4PGLaDCngvjqCoc9KGkxea4hgiq6qitk html/css
        added QmehoRufjC3xHPvTffyjMPgYgYMaaSzEwZgqnubv8oy5Mx html/etc
        added QmYhaNnLGtFDEc559T9bVkqYqaXLGojMWDzVqjFZgrmnCi html/img
        added Qme6hd6tYXTFb7bb7L3JZ5U6ygktpAHKxbaeffYyQN85mW html
        */
        Multihash HASH = Multihash.fromBase58("Qme6hd6tYXTFb7bb7L3JZ5U6ygktpAHKxbaeffYyQN85mW");
        
        // get 
        Path outPath = Paths.get("target/ipfs");
        Path path = client.get(HASH, outPath).get(3000, TimeUnit.MILLISECONDS);
        Assert.assertEquals(outPath.resolve(HASH.toBase58()), path);
        Assert.assertTrue("Is dir: " + path, path.toFile().isDirectory());
        Assert.assertTrue(path.resolve("index.html").toFile().isFile());
    }

    @Test
    public void onlyHash() throws Exception {
        
        /*
        $ ipfs add -r --only-hash ipfs/core/src/test/resources/contentA
        added QmctTBqcHf1A3uQhrTZgSqzV8Yh7nD9ud1tkPF58DPJFbw contentA/file03.txt
        added QmQrDwzQzvBSJaAyz9VFWKB8vjhYtjTUWBEBmkYuGtjQW3 contentA/subA/file01.txt
        added QmbEsRMKVUSjUYenh4gvw4UcqUGRGTGC7D221eU4ffpxLa contentA/subB/subC/file02.txt
        added QmUUcAcMN9PXJwzEJHEZA3EgL8MiJZUbftg5dfrPCDK6YB contentA/subA
        added QmT5zhCnhR73e8LnMs59j4tfn8TPH6kgL9ajiULsUJ9q6K contentA/subB/subC
        added QmP2oWphFGtPasCDXurRaxHZsq6NHnD2kd5Tt5H1NTEqSn contentA/subB
        added QmZBd64wnUqfpeaKFNTNqUxSZmzawD4pLi4k8GH6sYWJm8 contentA
        */
        Multihash HASH = Multihash.fromBase58("QmZBd64wnUqfpeaKFNTNqUxSZmzawD4pLi4k8GH6sYWJm8");
        
        // add --only-hash
        
        Path path = Paths.get("src/test/resources/contentA");
        List<Multihash> cids = client.add(path, true);
        Assert.assertEquals(7, cids.size());
        Assert.assertEquals(HASH, cids.get(6));
    }
}
