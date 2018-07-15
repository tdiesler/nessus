package io.nessus.test.ipfs;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.TimeoutException;

public class IPFSClientTest {

    private static final String TEST_HASH = "QmUD7uG5prAMHbcCfp4x1G1mMSpywcSMHTGpq62sbpDAg6";
    
    @Test
    public void version() throws Exception {
        IPFSClient client = new IPFSClient();
        String[] version = split(client.version());
        Assert.assertEquals("0.4.15", version[2]);
    }

    @Test
    public void testBasicOps() throws Exception {
        
        // add
        
        IPFSClient client = new IPFSClient();
        URL furl = getClass().getResource("/userfile.txt");
        Path path = Paths.get(furl.getPath());
        String res = client.add(path.toString());
        Assert.assertEquals(TEST_HASH, res);

        // cat 
        
        res = client.cat(TEST_HASH);
        Assert.assertEquals("The quick brown fox jumps over the lazy dog.", res);

        // get 
        
        Path tmpDir = Files.createTempDirectory(".aeg");
        String[] toks = split(client.get("-o " + tmpDir, TEST_HASH));
        path = Paths.get(toks[3], TEST_HASH);
        Assert.assertTrue("Is file: " + path, path.toFile().isFile());
    }

    @Test
    public void getAsync() throws Exception {

        String apiHost = System.getenv(IPFSClient.ENV_IPFS_API_HOST);
        Assume.assumeNotNull(apiHost);
        
        IPFSClient client = new IPFSClient(3L, TimeUnit.SECONDS);
        
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<String> future = service.submit(new Callable<String>() {
            public String call() throws Exception {
                client.get("QmUD7uG5prAMHbcCfp4x1G1mMSpywcSMHTGpq62sbpxxxx");
                return "invalid";
            }
        });
        
        try {
            future.get();
            Assert.fail("TimeoutException expected");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            Assert.assertTrue(cause.toString(), cause instanceof TimeoutException);
        }
    }

    private String[] split(String result) {
        return result.split(" ");
    }
}
