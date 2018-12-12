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

    public static final int DEFAULT_STRENGTH = 2048;

	static {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    /**
     * Generate the key pair from a random source.
     */
    public static KeyPair newKeyPair() throws GeneralSecurityException {
        return generateKeyPairInternal(null, DEFAULT_STRENGTH);
    }
    
    /**
     * Derive the key pair from blockchain private key.
     */
    public static KeyPair newKeyPair(Address addr) throws GeneralSecurityException {
        return generateKeyPairInternal(addr, DEFAULT_STRENGTH);
    }

    public static KeyPair newKeyPair(Address addr, int strenght) throws GeneralSecurityException {
        return generateKeyPairInternal(addr, strenght);
    }

    public static String encodeKey(Key key) {
        byte[] rawKey = key.getEncoded();
        return Base64.getEncoder().encodeToString(rawKey);
    }

    public static PublicKey decodePublicKey(String encKey) throws GeneralSecurityException {
        byte[] keyBytes = Base64.getDecoder().decode(encKey);
        return decodePublicKey(keyBytes);
    }

    static PublicKey decodePublicKey(byte[] keyBytes) throws GeneralSecurityException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private static KeyPair generateKeyPairInternal(Address addr, int strenght) throws GeneralSecurityException {
        AssertArgument.assertTrue(addr == null || addr.getPrivKey() != null, "Wallet does not control private key for: " + addr);
    	
        SecureRandom secrnd = addr != null ? new DeterministicRandom(addr) : new SecureRandom();
        
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        KeyPairGeneratorSpi spi = (KeyPairGeneratorSpi) kpg;
        spi.initialize(strenght, secrnd);
        
        KeyPair keyPair = kpg.generateKeyPair();
        
        // Normalize the key representation
        PublicKey pubKey = decodePublicKey(encodeKey(keyPair.getPublic()));
		return new KeyPair(pubKey, keyPair.getPrivate());
    }
}
