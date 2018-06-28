package io.nessus.cypher;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.Cipher;

import io.nessus.cypher.utils.DeterministicRandom;

/*
 * InvalidKeyException: Illegal key size
 * http://www.bouncycastle.org/wiki/display/JA1/Frequently+Asked+Questions
 * 
 * $ find /Library/Java/JavaVirtualMachines/jdk1.8.0_171.jdk -name *_policy.jar
 * 
 * Make sure these are on the classpath
 * 
 * .../jre/lib/security/policy/unlimited/local_policy.jar
 * .../jre/lib/security/policy/unlimited/US_export_policy.jar
 */
public class ECIESCipher {

    public KeyPair generateKeyPair() throws GeneralSecurityException {
        
        SecureRandom prng = new SecureRandom();
        return generateKeyPairInternal(prng);
    }
    
    public KeyPair generateKeyPair(byte[] key) throws GeneralSecurityException {
        
        SecureRandom prng = new DeterministicRandom(key);
        return generateKeyPairInternal(prng);
    }

    public byte[] encrypt(PublicKey pubKey, byte[] message) throws GeneralSecurityException {
        
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return cipher.doFinal(message);
    }
    
    public byte[] decrypt(PrivateKey pivKey, byte[] ciphertext) throws GeneralSecurityException {
        
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.DECRYPT_MODE, pivKey);
        return cipher.doFinal(ciphertext);
    }
    
    private KeyPair generateKeyPairInternal(SecureRandom prng) throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
        kpg.initialize(new ECGenParameterSpec("secp128r1"), prng);
        return kpg.generateKeyPair();
    }
}