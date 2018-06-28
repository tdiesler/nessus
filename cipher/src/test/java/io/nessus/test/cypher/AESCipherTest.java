package io.nessus.test.cypher;

import java.io.InputStream;

import javax.crypto.SecretKey;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.cypher.AESCipher;
import io.nessus.cypher.utils.StreamUtils;

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
