package io.nessus.ipfs.impl;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ipfs.multihash.Multihash;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.cipher.utils.RSAUtils;
import io.nessus.ipfs.IPFSClient;
import io.nessus.utils.AssertState;

class AddrRegistration {
	
    static final Logger LOG = LoggerFactory.getLogger(AddrRegistration.class);

	private static final String KEY_LABEL = "Label";
	private static final String KEY_ADDRESS = "Address";
	private static final String KEY_PUBKEY = "PublicKey";
	
	private final FHeaderValues fhvals;
	private final Address addr;
	private final PublicKey pubKey;
	
	AddrRegistration(FHeaderValues fhvals, Address addr, PublicKey pubKey) {
		this.fhvals = fhvals;
		this.addr = addr;
		this.pubKey = pubKey;
		
        List<String> labels = addr.getLabels();
        AssertState.assertTrue(labels.size() < 2, "Multiple labels: " + labels);
	}
	
	static AddrRegistration fromIpfs(IPFSClient ipfsClient, Wallet wallet, FHeaderValues fhvals, Multihash cid) throws IOException, GeneralSecurityException {
		
		Properties props = new Properties();
    	InputStream input = ipfsClient.cat(cid.toBase58());
        props.load(input);
        
        String version = props.getProperty(fhvals.PREFIX + "-Version");
        String rawAddr = props.getProperty(KEY_ADDRESS);
        String encKey = props.getProperty(KEY_PUBKEY);
        
        AssertState.assertEquals(fhvals.VERSION, version);
        
        Address addr = wallet.findAddress(rawAddr);
        PublicKey pubKey = RSAUtils.decodePublicKey(encKey);
        return new AddrRegistration(fhvals, addr, pubKey);
	}

	Address getAddress() {
		return addr;
	}
	
	String getLabel() {
		return addr.getLabels().get(0);
	}

	PublicKey getPubKey() {
		return pubKey;
	}

	String addIpfsContent(IPFSClient ipfsClient) throws IOException {
		
        Properties props = new Properties();
        props.setProperty(fhvals.PREFIX + "-Version", fhvals.VERSION);
        props.setProperty(KEY_LABEL, getLabel());
        props.setProperty(KEY_ADDRESS, addr.getAddress());
        props.setProperty(KEY_PUBKEY, RSAUtils.encodeKey(pubKey));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.store(new OutputStreamWriter(baos), null);
        
        String cid = ipfsClient.addSingle(baos.toByteArray());
        
        LOG.info("Addr Registration: {} => {} => {}", addr.getAddress(), cid, pubKey);
        return cid;
	}
	
	public String toString() {
        return String.format("[version=%s, addr=%s, pubKey=%s]", fhvals.VERSION, addr, pubKey);
	}
}