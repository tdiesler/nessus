package io.nessus.cipher.utils;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.nessus.Wallet.Address;

public class ECIESUtils {

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
        SecureRandom secrnd = new DeterministicRandom(addr);
        return generateKeyPairInternal(secrnd);
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
    public static PublicKey decodePublicKey(String encKey) {
        byte[] keyBytes = Base64.getDecoder().decode(encKey);
        return decodePublicKey(keyBytes);
    }

    static PublicKey decodePublicKey(byte[] keyBytes) {
        return new PublicKey() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getFormat() {
                return "X.509";
            }

            @Override
            public byte[] getEncoded() {
                return keyBytes;
            }

            @Override
            public String getAlgorithm() {
                return "EC";
            }
            
            @Override
            public int hashCode() {
                return Arrays.hashCode(keyBytes);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof PublicKey)) return false;
                PublicKey other = (PublicKey) obj;
                return Arrays.equals(keyBytes, other.getEncoded());
            }
            
            public String toString() {
                return Base64.getEncoder().encodeToString(keyBytes);
            }
        };
    }
    
    private static KeyPair generateKeyPairInternal(SecureRandom secrnd) throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
        kpg.initialize(new ECGenParameterSpec("secp128r1"), secrnd);
        return kpg.generateKeyPair();
    }
}
