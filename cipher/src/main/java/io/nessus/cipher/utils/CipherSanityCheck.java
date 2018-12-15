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
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.nessus.AbstractAddress;
import io.nessus.Wallet.Address;
import io.nessus.cipher.RSACipher;
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
        
        // Generate a deterministic key pair

        // This is a Bitcoin priv key in WIF
        Address auxAddr = getTestAddress();
        
        KeyPair keyPair = RSAUtils.newKeyPair(auxAddr);
        PublicKey pubKey = keyPair.getPublic();
        PrivateKey pivKey = keyPair.getPrivate();

        // Verify that the EC public key is deterministic
        
        final String encPubKey = RSAUtils.encodeKey(pubKey);
        AssertState.assertEquals("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5SSQgSaIsFYGKxp9uMuwnYi/M1SEx9uq74suJkdbiwUF/Yznz+bu6ZhpimE79lG/g3rn2ptcWXqZ8DcKOucIyCN2JjmRxh5zpRrR9mfV8JYgvYdjLDweUTABidR3w9FkMKrv+1akyz3S/faxy46xl2L10YlC+g3ufLeWrXEjDZckcBJSYSe1KCateAnSSfm/8I733Lr75mBptlPoCdQF5TrfBbkTS7oEcUkk6Mf3ZMpk7Q/QVU7FnK0+JY0kiZruiobSS3WVGCwZaOjKlw/m3PvdW+s/2Ts26Mr1u8HthHk5Bz/GD8SqIfFPvaebYfUpNk6ct8Y6mNNVfKkk+2Fr+QIDAQAB", encPubKey);
        AssertState.assertEquals(294, pubKey.getEncoded().length);
        
        pubKey = RSAUtils.decodePublicKey(encPubKey);

        RSACipher ecipher = new RSACipher();
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
