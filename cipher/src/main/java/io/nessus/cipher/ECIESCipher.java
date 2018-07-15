package io.nessus.cipher;

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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.Cipher;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.nessus.cipher.utils.DeterministicRandom;

public class ECIESCipher {

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    public KeyPair generateKeyPair() throws GeneralSecurityException {
        
        SecureRandom prng = new SecureRandom();
        return generateKeyPairInternal(prng);
    }
    
    public KeyPair generateKeyPair(byte[] seedBytes) throws GeneralSecurityException {
        
        SecureRandom prng = new DeterministicRandom(seedBytes);
        return generateKeyPairInternal(prng);
    }

    public byte[] encrypt(PublicKey pubKey, byte[] message) throws GeneralSecurityException {
        
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return cipher.doFinal(message);
    }
    
    public byte[] decrypt(PrivateKey privKey, byte[] ciphertext) throws GeneralSecurityException {
        
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.DECRYPT_MODE, privKey);
        return cipher.doFinal(ciphertext);
    }
    
    private KeyPair generateKeyPairInternal(SecureRandom prng) throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
        kpg.initialize(new ECGenParameterSpec("secp128r1"), prng);
        return kpg.generateKeyPair();
    }
}
