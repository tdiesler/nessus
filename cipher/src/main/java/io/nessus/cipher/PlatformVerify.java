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

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;

import io.nessus.AssertState;

public class PlatformVerify {

    public static void main(String[] args) throws Exception {
        
        Security.addProvider(new BouncyCastleProvider());

        // Generate an AES secret
        
        AESCipher aes = new AESCipher();
        byte[] encBytes = aes.getSecretKey().getEncoded();
        String encSecret = Base64.getEncoder().encodeToString(encBytes);
        
        // Generate a deterministic key pair using EC

        // This is a Bitcoin priv key in WIF
        String privKey = "cVfiZLCWbCm3SWoBAToaCoMuYJJjEw5cR6ifuWQY1a5wadXynGC2";
        byte[] seed = Arrays.reverse(privKey.getBytes());
        
        ECIESCipher ecies = new ECIESCipher();
        KeyPair keyPair = ecies.generateKeyPair(seed);
        PublicKey pubKey = keyPair.getPublic();
        PrivateKey pivKey = keyPair.getPrivate();

        // Verify that the EC public key is deterministic
        
        final String encPubKey = Base64.getEncoder().encodeToString(pubKey.getEncoded());
        AssertState.assertEquals("MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAEoQu96WAI2FFlR+6F5AEkvomjkHSPHo4owSut2em3FP8=", encPubKey);
        AssertState.assertEquals(56, pubKey.getEncoded().length);
        
        pubKey = new PublicKey() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getFormat() {
                return "X.509";
            }

            @Override
            public byte[] getEncoded() {
                return Base64.getDecoder().decode(encPubKey);
            }

            @Override
            public String getAlgorithm() {
                return "EC";
            }
        };

        byte[] ciphertext = ecies.encrypt(pubKey, encBytes);
        byte[] decrypted = ecies.decrypt(pivKey, ciphertext);

        String encResult = Base64.getEncoder().encodeToString(decrypted);
        System.out.println(encSecret + " => " + encResult);
        AssertState.assertEquals(encSecret, encResult);
    }
}
