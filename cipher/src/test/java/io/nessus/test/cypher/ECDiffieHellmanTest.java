package io.nessus.test.cypher;

import java.security.KeyPair;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Test;

import io.nessus.cypher.ECDH;

public class ECDiffieHellmanTest {

    @Test
    public void testECDH() throws Exception {

        Security.addProvider(new BouncyCastleProvider());
        
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
