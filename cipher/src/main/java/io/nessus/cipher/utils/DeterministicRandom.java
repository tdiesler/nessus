package io.nessus.cipher.utils;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;

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

import java.security.SecureRandom;
import java.util.Base64;

import org.bouncycastle.util.Arrays;

import io.nessus.Wallet.Address;
import io.nessus.utils.AssertArgument;

@SuppressWarnings("serial") 
public class DeterministicRandom extends SecureRandom {
    
    private final MessageDigest md;
    private byte[] input;
    
    public DeterministicRandom(Address addr) throws GeneralSecurityException {
        AssertArgument.assertNotNull(addr.getPrivKey(), "Wallet does not control private key for: " + addr);
        
        md = MessageDigest.getInstance("SHA-256");
        input = md.digest(Base64.getDecoder().decode(addr.getPrivKey()));
    }

    public DeterministicRandom(byte[] digest) throws GeneralSecurityException {
        this.md = MessageDigest.getInstance("SHA-256");
        this.input = digest;
    }

    @Override
    public void nextBytes(byte[] buffer) {
        
        int idx = 0;
        byte[] seed = Arrays.clone(input);
        while (seed.length < buffer.length) {
            idx = (idx + 3) % input.length;
            byte[] head = Arrays.copyOfRange(input, 0, idx);
            byte[] tail = Arrays.copyOfRange(input, idx, input.length);
            input = Arrays.concatenate(tail, head);
            seed = Arrays.concatenate(seed, md.digest(input));
        }
        
        System.arraycopy(seed, 0, buffer, 0, buffer.length);
    }
}
