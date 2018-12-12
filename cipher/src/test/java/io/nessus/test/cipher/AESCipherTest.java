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
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.cipher.AESCipher;
import io.nessus.cipher.utils.AESUtils;

public class AESCipherTest extends AbstractCipherTest {

    @Test
    public void testNewKeyEveryTime() throws Exception {
        
        List<String> tokens = new ArrayList<>();
        List<String> secmsgs = new ArrayList<>();
        
        int count = 3;
        for (int i = 0; i < count; i++) {
            AESCipher acipher = new AESCipher();
            SecretKey secKey = AESUtils.newSecretKey();
            tokens.add(AESUtils.encodeKey(secKey));
            InputStream ins = asStream(text);
            InputStream secIns = acipher.encrypt(secKey, ins);
            secmsgs.add(encode(secIns));
        }
        
        // Note, this gives a new encryption token every time
        
        Assert.assertEquals(count, tokens.stream()
                .peek(tok -> LOG.info(tok))
                .distinct().count());
        
        // Note, this gives a new different secret message every time
        
        Assert.assertEquals(count, secmsgs.stream()
                .peek(tok -> LOG.info(tok))
                .distinct().count());
        
        List<String> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AESCipher acipher = new AESCipher();
            SecretKey secKey = AESUtils.decodeSecretKey(tokens.get(i));
            InputStream secIns = asStream(decode(secmsgs.get(i)));
            InputStream msgIns = acipher.decrypt(secKey, secIns);
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
        
        SecretKey secKey = AESUtils.newSecretKey();
        String token = AESUtils.encodeKey(secKey);
        LOG.info(token);
        
        List<String> secmsgs = new ArrayList<>();
        
        int count = 3;
        for (int i = 0; i < count; i++) {
            AESCipher acipher = new AESCipher();
            InputStream ins = asStream(text);
            InputStream secIns = acipher.encrypt(secKey, ins);
            secmsgs.add(encode(secIns));
        }
        
        // Note, this gives a new different secret message every time
        
        Assert.assertEquals(count, secmsgs.stream()
                .peek(tok -> LOG.info(tok))
                .distinct().count());
        
        List<String> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AESCipher acipher = new AESCipher();
            secKey = AESUtils.decodeSecretKey(token);
            InputStream secIns = asStream(decode(secmsgs.get(i)));
            InputStream msgIns = acipher.decrypt(secKey, secIns);
            results.add(asString(msgIns));
        }
        
        // Verify that the result is always the same
        
        Assert.assertEquals(1, results.stream()
                .peek(msg -> LOG.info(msg))
                .distinct().count());
        
        Assert.assertEquals(text, results.get(0));
    }

    @Test
    public void testContentBasedIV() throws Exception {
        
        SecretKey secKey = AESUtils.newSecretKey();
        String token = AESUtils.encodeKey(secKey);
        LOG.info(token);
        
        List<String> secmsgs = new ArrayList<>();
        
        int count = 3;
        for (int i = 0; i < count; i++) {
            AESCipher acipher = new AESCipher();
            InputStream ins = asStream(text);
            byte[] iv = AESUtils.getIV(cid, addrBob);
            InputStream secIns = acipher.encrypt(secKey, iv, ins, null);
            secmsgs.add(encode(secIns));
        }
        
        // Note, this gives the same secret message every time
        
        Assert.assertEquals(1, secmsgs.stream()
                .peek(tok -> LOG.info(tok))
                .distinct().count());
        
        List<String> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AESCipher acipher = new AESCipher();
            secKey = AESUtils.decodeSecretKey(token);
            InputStream secIns = asStream(decode(secmsgs.get(i)));
            InputStream msgIns = acipher.decrypt(secKey, secIns);
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
        
        SecretKey secKey = AESUtils.newSecretKey(addrBob, cid);
        String token = AESUtils.encodeKey(secKey);
        
        Assert.assertEquals("QXqjvsbKAU2Moc+6sWk/4A==", token);
        
        List<String> secmsgs = new ArrayList<>();
        
        int count = 3;
        for (int i = 0; i < count; i++) {
            AESCipher acipher = new AESCipher();
            InputStream ins = asStream(text);
            byte[] iv = AESUtils.getIV(cid, addrBob);
            InputStream secIns = acipher.encrypt(secKey, iv, ins, null);
            secmsgs.add(encode(secIns));
        }
        
        // Note, this gives the same secret message every time
        
        Assert.assertEquals(1, secmsgs.stream()
                .peek(tok -> LOG.info(tok))
                .distinct().count());
        
        List<String> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AESCipher acipher = new AESCipher();
            secKey = AESUtils.decodeSecretKey(token);
            InputStream secIns = asStream(decode(secmsgs.get(i)));
            InputStream msgIns = acipher.decrypt(secKey, secIns);
            results.add(asString(msgIns));
        }
        
        // Verify that the result is always the same
        
        Assert.assertEquals(1, results.stream()
                .peek(msg -> LOG.info(msg))
                .distinct().count());
        
        Assert.assertEquals(text, results.get(0));
    }

    @Test
    public void testKeyStrenght() throws Exception {
        
        SecretKey secKey = AESUtils.newSecretKey(addrBob, cid, 512);
        String token = AESUtils.encodeKey(secKey);
        LOG.info("{}", token);
        
        Assert.assertEquals(88, token.length());
    }
}
