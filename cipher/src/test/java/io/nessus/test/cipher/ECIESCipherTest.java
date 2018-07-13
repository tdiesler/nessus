package io.nessus.test.cipher;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

import io.nessus.cipher.AESCipher;
import io.nessus.cipher.ECIESCipher;

public class ECIESCipherTest {

    @Test
    public void testECIES() throws Exception {

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
        Assert.assertEquals("MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAEoQu96WAI2FFlR+6F5AEkvomjkHSPHo4owSut2em3FP8=", encPubKey);
        Assert.assertEquals(56, pubKey.getEncoded().length);

        // Restore the EC pubKey from base64 encoded string
        // Note, this requiers unlimited security policies
        
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
        Assert.assertEquals(encSecret, encResult);

    }
}
