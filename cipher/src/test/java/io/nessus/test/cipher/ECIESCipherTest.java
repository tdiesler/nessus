package io.nessus.test.cipher;

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

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.cipher.ECIESCipher;
import io.nessus.cipher.utils.ECIESUtils;

public class ECIESCipherTest extends AbstractCipherTest {

    @Test
    public void testNewKeyEveryTime() throws Exception {
        
        List<KeyPair> keys = new ArrayList<>();
        List<String> secmsgs = new ArrayList<>();
        
        int count = 3;
        for (int i = 0; i < count; i++) {
            ECIESCipher acipher = new ECIESCipher();
            KeyPair keyPair = ECIESUtils.newKeyPair();
            keys.add(keyPair);
            InputStream ins = asStream(text);
            PublicKey pubKey = keyPair.getPublic();
            InputStream secIns = acipher.encrypt(pubKey, ins);
            secmsgs.add(encode(secIns));
        }
        
        // Note, this gives a new encryption token every time
        
        Assert.assertEquals(count, keys.stream()
                .peek(key -> LOG.info(encode(key.getPublic().getEncoded())))
                .distinct().count());
        
        // Note, this gives a new different secret message every time
        
        Assert.assertEquals(count, secmsgs.stream()
                .peek(tok -> LOG.info(tok))
                .distinct().count());
        
        List<String> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ECIESCipher acipher = new ECIESCipher();
            PrivateKey privKey = keys.get(i).getPrivate();
            InputStream secIns = asStream(decode(secmsgs.get(i)));
            InputStream msgIns = acipher.decrypt(privKey, secIns);
            results.add(asString(msgIns));
        }
        
        // Verify that the result is always the same
        
        Assert.assertEquals(1, results.stream()
                .peek(msg -> LOG.info(msg))
                .distinct().count());
        
        Assert.assertEquals(text, results.get(0));
    }

    @Test
    public void testKeyReuse() throws Exception {
        
        KeyPair keyPair = ECIESUtils.newKeyPair();
        PublicKey pubKey = keyPair.getPublic();
        String token = encode(pubKey.getEncoded());
        LOG.info(token);
        
        List<String> secmsgs = new ArrayList<>();
        
        int count = 3;
        for (int i = 0; i < count; i++) {
            ECIESCipher acipher = new ECIESCipher();
            InputStream ins = asStream(text);
            InputStream secIns = acipher.encrypt(pubKey, ins);
            secmsgs.add(encode(secIns));
        }
        
        // Note, this gives a new different secret message every time
        
        Assert.assertEquals(count, secmsgs.stream()
                .peek(tok -> LOG.info(tok))
                .distinct().count());
        
        List<String> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ECIESCipher acipher = new ECIESCipher();
            PrivateKey privKey = keyPair.getPrivate();
            InputStream secIns = asStream(decode(secmsgs.get(i)));
            InputStream msgIns = acipher.decrypt(privKey, secIns);
            results.add(asString(msgIns));
        }
        
        // Verify that the result is always the same
        
        Assert.assertEquals(1, results.stream()
                .peek(msg -> LOG.info(msg))
                .distinct().count());
        
        Assert.assertEquals(text, results.get(0));
    }


    @Test
    public void testDeterministicKey() throws Exception {
        
        KeyPair keyPair = ECIESUtils.newKeyPair(addrBob);
        PublicKey pubKey = keyPair.getPublic();
        String token = encode(pubKey.getEncoded());
        LOG.info(token);
        
        Assert.assertEquals("MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAEYtzTFMrUdiyYkmsTzP1fAgDmMEzvzuvo3Wp+Eya1JU8=", token);
        Assert.assertEquals(76, token.length());
        
        List<String> secmsgs = new ArrayList<>();
        
        int count = 3;
        for (int i = 0; i < count; i++) {
            ECIESCipher acipher = new ECIESCipher();
            InputStream ins = asStream(text);
            InputStream secIns = acipher.encrypt(pubKey, ins);
            secmsgs.add(encode(secIns));
        }
        
        // Note, this gives a new different secret message every time
        
        Assert.assertEquals(count, secmsgs.stream()
                .peek(tok -> LOG.info(tok))
                .distinct().count());
        
        List<String> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ECIESCipher acipher = new ECIESCipher();
            PrivateKey privKey = keyPair.getPrivate();
            InputStream secIns = asStream(decode(secmsgs.get(i)));
            InputStream msgIns = acipher.decrypt(privKey, secIns);
            results.add(asString(msgIns));
        }
        
        // Verify that the result is always the same
        
        Assert.assertEquals(1, results.stream()
                .peek(msg -> LOG.info(msg))
                .distinct().count());
        
        Assert.assertEquals(text, results.get(0));
    }
}
