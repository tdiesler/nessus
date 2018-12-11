package io.nessus.cipher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;

import io.nessus.utils.StreamUtils;

public class RSACipher {

    public InputStream encrypt(PublicKey pubKey, InputStream input) throws GeneralSecurityException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamUtils.copyStream(input, baos);
        return new ByteArrayInputStream(encrypt(pubKey, baos.toByteArray()));
    }
    
    public byte[] encrypt(PublicKey pubKey, byte[] message) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return cipher.doFinal(message);
    }
    
    public InputStream decrypt(PrivateKey privKey, InputStream input) throws GeneralSecurityException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamUtils.copyStream(input, baos);
        return new ByteArrayInputStream(decrypt(privKey, baos.toByteArray()));
    }
    
    public byte[] decrypt(PrivateKey privKey, byte[] ciphertext) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA", "BC");
        cipher.init(Cipher.DECRYPT_MODE, privKey);
        return cipher.doFinal(ciphertext);
    }
}
