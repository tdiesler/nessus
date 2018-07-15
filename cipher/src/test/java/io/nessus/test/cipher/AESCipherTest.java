package io.nessus.test.cipher;

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

import java.io.InputStream;

import javax.crypto.SecretKey;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.cipher.AESCipher;
import io.nessus.cipher.utils.StreamUtils;

public class AESCipherTest {

    @Test
    public void testAES() throws Exception {

        InputStream userInput = getClass().getResourceAsStream("/userfile.txt");

        AESCipher cipher = new AESCipher();
        SecretKey encToken = cipher.getSecretKey();

        // ENCRYPT

        InputStream secretMessage = cipher.encrypt(encToken, null, userInput);

        // Verify that the encryption token can be reconstructed
        String encoded = cipher.encodeSecretKey(encToken);
        encToken = cipher.decodeSecretKey(encoded);
        
        // DECRYPT
        
        InputStream plainMessage = cipher.decrypt(encToken, null, secretMessage);
        
        String result = new String (StreamUtils.toBytes(plainMessage));
        Assert.assertEquals("The quick brown fox jumps over the lazy dog", result);
    }
}
