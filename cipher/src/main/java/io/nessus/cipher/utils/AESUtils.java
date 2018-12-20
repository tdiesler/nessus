package io.nessus.cipher.utils;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;

import io.ipfs.multihash.Multihash;
import io.nessus.Wallet.Address;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;

public class AESUtils {

    public static final int DEFAULT_STRENGTH = 128;
    
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    /**
     * Generate the secret key from a random source.
     */
    public static SecretKey newSecretKey() throws GeneralSecurityException {
        return generateSecretKeyInternal(null, null, DEFAULT_STRENGTH);
    }
    
    /**
     * Create an AES secret key derived from the owner's private key.
     */
    public static SecretKey newSecretKey(Address addr) throws GeneralSecurityException {
        return generateSecretKeyInternal(addr, null, DEFAULT_STRENGTH);
    }

    /**
     * Create an AES secret key derived from the owner's private key and some content id.
     */
    public static SecretKey newSecretKey(Address addr, Multihash cid) throws GeneralSecurityException {
        return generateSecretKeyInternal(addr, cid, DEFAULT_STRENGTH);
    }

    public static SecretKey newSecretKey(Address addr, Multihash cid, int strength) throws GeneralSecurityException {
        return generateSecretKeyInternal(addr, cid, strength);
    }

    public static String encodeKey(Key key) {
        byte[] rawKey = key.getEncoded();
        return Base64.getEncoder().encodeToString(rawKey);
    }

    public static SecretKey decodeSecretKey(String encKey) {
        byte[] rawKey = Base64.getDecoder().decode(encKey);
        return decodeSecretKey(rawKey);
    }

    public static SecretKey decodeSecretKey(byte[] rawKey) {
        AssertArgument.assertTrue(rawKey.length == 16, "Expected 128 bit");
        return new SecretKeySpec(rawKey, "AES");
    }

    /**
     * Create an AES initialization vector derived from the owner's private key and a content id.
     * 
     * The idea is that the same owner can encrypt the same content repeatetly and
     * still get identical encrypted content.
     */
	public static byte[] getIV(Address addr, Multihash cid) throws GeneralSecurityException {
        AssertArgument.assertNotNull(addr.getPrivKey(), "Wallet does not control private key for: " + addr);

        // Decode the priv key as base64 (even though it might be WIF encoded)
        // Note, the reverse order of input data wrt the secret key
        byte[] rawSeed = Base64.getDecoder().decode(cid.toBase58() + addr.getPrivKey());

        // Use a secure hash of the raw seed as the pseudo random seed
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] sha256 = md.digest(rawSeed);
        AssertState.assertTrue(sha256.length >= 12, "IV seed too short: " + sha256.length);

        // NEVER REUSE THIS IV WITH SAME KEY
        return Arrays.copyOf(sha256, 12);
	}

    private static SecretKey generateSecretKeyInternal(Address addr, Multihash cid, int strenght) throws GeneralSecurityException {
        AssertArgument.assertTrue(addr == null || addr.getPrivKey() != null, "Wallet does not control private key for: " + addr);
        AssertArgument.assertTrue(strenght % 8 == 0, "Invalid stregth: " + strenght);
    	
        SecureRandom secrnd;
        if (addr != null && cid != null) {
        	secrnd = new DeterministicRandom(addr, cid);
        } else if (addr != null) {
        	secrnd = new DeterministicRandom(addr);
        } else {
        	secrnd = new SecureRandom();
        }
        
        byte[] key = new byte[strenght / 8];
        secrnd.nextBytes(key);
        
        return new SecretKeySpec(key, "AES");
    }
}
