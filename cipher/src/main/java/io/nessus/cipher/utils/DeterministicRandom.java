package io.nessus.cipher.utils;

/*-
 * #%L
 * Nessus :: Cipher
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

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import org.bouncycastle.util.Arrays;

import io.ipfs.multihash.Multihash;
import io.nessus.Wallet.Address;
import io.nessus.utils.AssertArgument;

@SuppressWarnings("serial") 
public class DeterministicRandom extends SecureRandom {
    
    private final MessageDigest md = MessageDigest.getInstance("SHA3-512");
    private byte[] digest;
    private long total;
    
    public DeterministicRandom(Address addr) throws GeneralSecurityException {
    	this (addr, null);
    }

    public DeterministicRandom(Address addr, Multihash cid) throws GeneralSecurityException {
		AssertArgument.assertNotNull(addr.getPrivKey(), "Wallet does not control private key for: " + addr);
        
        byte[] bytesA = addr.getPrivKey().getBytes();
        byte[] bytesB = cid != null ? cid.toBytes() : Arrays.reverse(bytesA);
		digest = Arrays.concatenate(md.digest(bytesA), md.digest(bytesB));
    }

    public DeterministicRandom(byte[] input) throws GeneralSecurityException {
        digest = md.digest(input);
    }

    @Override
    public void nextBytes(byte[] buffer) {
        
    	if (512 * 1024 < total)
    		throw new IllegalStateException("Upper limit exceeded");
    	
    	int idx = 0;
        byte[] seed = new byte[0];
        while (seed.length < buffer.length) {
            idx = (idx + 7) % digest.length;
            byte[] head = Arrays.copyOfRange(digest, 0, idx);
            byte[] tail = Arrays.copyOfRange(digest, idx, digest.length);
            digest = Arrays.concatenate(tail, head);
            seed = Arrays.concatenate(seed, md.digest(digest));
        }
        
        System.arraycopy(seed, 0, buffer, 0, buffer.length);
        total += buffer.length;
    }
}
