package io.nessus.cipher.utils;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyPairGeneratorSpi;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.nessus.Wallet.Address;
import io.nessus.utils.AssertArgument;

public class RSAUtils {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    /**
     * Generate the key pair from a random source.
     */
    public static KeyPair newKeyPair() throws GeneralSecurityException {
        
        SecureRandom secrnd = new SecureRandom();
        return generateKeyPairInternal(secrnd);
    }
    
    /**
     * Derive the key pair from blockchain private key.
     */
    public static KeyPair getKeyPair(Address addr) throws GeneralSecurityException {
        AssertArgument.assertNotNull(addr.getPrivKey(), "Wallet does not control private key for: " + addr);

        // Derive the corresponding deterministic EC key pair  
        SecureRandom secrnd = new DeterministicRandom(addr);
        KeyPair keyPair = generateKeyPairInternal(secrnd);

        return keyPair;
    }

    /**
     * Encode the given key
     */
    public static String encodeKey(Key key) {
        byte[] rawKey = key.getEncoded();
        return Base64.getEncoder().encodeToString(rawKey);
    }

    /**
     * Decode public key from the given base64 encoded string
     * 
     * Note, this requiers unlimited security policies
     */
    public static PublicKey decodePublicKey(String encKey) throws GeneralSecurityException {
        byte[] keyBytes = Base64.getDecoder().decode(encKey);
        return decodePublicKey(keyBytes);
    }

    static PublicKey decodePublicKey(byte[] keyBytes) throws GeneralSecurityException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private static KeyPair generateKeyPairInternal(SecureRandom secrnd) throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        KeyPairGeneratorSpi spi = (KeyPairGeneratorSpi) kpg;
        spi.initialize(2048, secrnd);
        return kpg.generateKeyPair();
    }
}
