package io.nessus.test.cypher;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

import io.nessus.cypher.AESCipher;
import io.nessus.cypher.ECIESCipher;

public class ECIESCipherTest {

    @Test
    public void testECIES() throws Exception {

        Security.addProvider(new BouncyCastleProvider());

        // Get the the token used to encrypt the user data
        AESCipher aes = new AESCipher();
        byte[] token = aes.getSecretKey().getEncoded();
        String encToken = Base64.getEncoder().encodeToString(token);
        
        // Generate a key pair using EC

        // This is a Bitcoin priv key in WIF
        String privKey = "cVfiZLCWbCm3SWoBAToaCoMuYJJjEw5cR6ifuWQY1a5wadXynGC2";
        byte[] bytes = Arrays.reverse(privKey.getBytes());
        
        ECIESCipher ecies = new ECIESCipher();
        KeyPair keyPair = ecies.generateKeyPair(bytes);
        PublicKey pubKey = keyPair.getPublic();
        PrivateKey pivKey = keyPair.getPrivate();

        // Verify that the public key is deterministic
        
        String encPubKey = Base64.getEncoder().encodeToString(pubKey.getEncoded());
        Assert.assertEquals("MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAEoQu96WAI2FFlR+6F5AEkvomjkHSPHo4owSut2em3FP8=", encPubKey);
        Assert.assertEquals(56, pubKey.getEncoded().length);
        
        byte[] ciphertext = ecies.encrypt(pubKey, token);
        byte[] decrypted = ecies.decrypt(pivKey, ciphertext);

        String encResult = Base64.getEncoder().encodeToString(decrypted);
        Assert.assertEquals(encToken, encResult);

    }
}
