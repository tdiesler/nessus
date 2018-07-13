package io.nessus.test.cipher;

import java.security.KeyPair;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.cipher.ECDH;

public class ECDiffieHellmanTest {

    @Test
    public void testECDH() throws Exception {

        ECDH ecdh = new ECDH();

        KeyPair pairA = ecdh.generateKeyPair();
        KeyPair pairB = ecdh.generateKeyPair();
        byte[] prvA = ecdh.savePrivateKey(pairA.getPrivate());
        byte[] pubA = ecdh.savePublicKey(pairA.getPublic());
        byte[] prvB = ecdh.savePrivateKey(pairB.getPrivate());
        byte[] pubB = ecdh.savePublicKey(pairB.getPublic());

        byte[] secretA = ecdh.generateSecret(prvA, pubB);
        byte[] secretB = ecdh.generateSecret(prvB, pubA);
        Assert.assertArrayEquals(secretA, secretB);
    }
}
