package io.nessus.cipher.utils;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
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

    private static KeyPair generateKeyPairInternal(SecureRandom secrnd) throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        KeyPairGeneratorSpi spi = (KeyPairGeneratorSpi) kpg;
        spi.initialize(2048, secrnd);
        return kpg.generateKeyPair();
    }
}
