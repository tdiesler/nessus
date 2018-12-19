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

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.math.BigDecimal;
import java.nio.file.Path;

import org.junit.Before;

import io.nessus.Wallet.Address;
import io.nessus.bitcoin.AbstractBitcoinTest;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.jaxrs.JAXRSConfig;
import io.nessus.ipfs.jaxrs.JAXRSConfig.JAXRSConfigBuilder;
import io.nessus.utils.FileUtils;

public abstract class AbstractJAXRSTest extends AbstractBitcoinTest {

	protected static JAXRSConfig config;
	protected static IPFSClient ipfsClient;
    
    @Before
    public void before() throws Exception {
    	super.before();

    	if (ipfsClient == null) {
    		
            config = new JAXRSConfigBuilder()
            		.bcurl(DEFAULT_JSONRPC_REGTEST_URL)
            		.build();
            
            ipfsClient = config.getIPFSClient();
            
            // Delete all local files
            Path rootPath = config.getDataDir();
            FileUtils.recursiveDelete(rootPath);
            
            // Give Bob & Mary some funds
            wallet.sendToAddress(addrBob.getAddress(), new BigDecimal("1.0"));
            wallet.sendToAddress(addrMary.getAddress(), new BigDecimal("1.0"));
            generate(1, addrSink);
    	}
    }

	Path getRootPath() {
        Path rootPath = config.getDataDir();
        rootPath.toFile().mkdirs();
        return rootPath;
    }

    Path getPlainPath(Address owner) {
		Path path = getRootPath().resolve("plain").resolve(owner.getAddress());
        path.toFile().mkdirs();
        return path;
    }
}
