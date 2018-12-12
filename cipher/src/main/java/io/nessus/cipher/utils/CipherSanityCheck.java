package io.nessus.cipher.utils;

import java.security.GeneralSecurityException;

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

import io.nessus.AbstractAddress;
import io.nessus.Wallet.Address;
import io.nessus.cipher.ECIESCipher;
import io.nessus.utils.AssertState;

public class CipherSanityCheck {

    public static void main(String[] args) throws Exception {
        
        verifyPlatform();
    }

    public static void verifyPlatform() throws GeneralSecurityException {
        
        Security.addProvider(new BouncyCastleProvider());

        // Generate an AES secret
        
        byte[] encBytes = AESUtils.newSecretKey().getEncoded();
        String encSecret = Base64.getEncoder().encodeToString(encBytes);
        
        // Generate a deterministic key pair using EC

        // This is a Bitcoin priv key in WIF
        Address auxAddr = getTestAddress();
        
        KeyPair keyPair = ECIESUtils.newKeyPair(auxAddr);
        PublicKey pubKey = keyPair.getPublic();
        PrivateKey pivKey = keyPair.getPrivate();

        // Verify that the EC public key is deterministic
        
        final String encPubKey = Base64.getEncoder().encodeToString(pubKey.getEncoded());
        AssertState.assertEquals("MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAEYtzTFMrUdiyYkmsTzP1fAgDmMEzvzuvo3Wp+Eya1JU8=", encPubKey);
        AssertState.assertEquals(56, pubKey.getEncoded().length);
        
        pubKey = ECIESUtils.decodePublicKey(encPubKey);

        ECIESCipher ecipher = new ECIESCipher();
        byte[] ciphertext = ecipher.encrypt(pubKey, encBytes);
        byte[] decrypted = ecipher.decrypt(pivKey, ciphertext);

        String encResult = Base64.getEncoder().encodeToString(decrypted);
        AssertState.assertEquals(encSecret, encResult);
    }

    private static Address getTestAddress() {
        Address addrBob = new AbstractAddress("n3ha6rJa8ZS7B4v4vwNWn8CnLHfUYXW1XE") {
            public String getPrivKey() {
                return "cVfiZLCWbCm3SWoBAToaCoMuYJJjEw5cR6ifuWQY1a5wadXynGC2";
            }
        };
        return addrBob;
    }
}
