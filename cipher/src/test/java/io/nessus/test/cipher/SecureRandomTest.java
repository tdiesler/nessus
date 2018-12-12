package io.nessus.test.cipher;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;

import io.nessus.cipher.utils.DeterministicRandom;

public class SecureRandomTest extends AbstractCipherTest {

    @Test
    public void testSecureRandom() throws Exception {
        
        List<String> tokens = new ArrayList<>();
        
        int count = 3;
        for (int i = 0; i < count; i++) {
        	SecureRandom secrnd = new SecureRandom();
        	byte[] buffer = new byte[16];
        	secrnd.nextBytes(buffer);
        	tokens.add(Hex.toHexString(buffer));
        }
        
        Assert.assertEquals(count, tokens.stream()
                .peek(tok -> LOG.info(tok))
                .distinct().count());
        
    }
    
    @Test
    public void testSeededSecureRandom() throws Exception {
        
        List<String> tokens = new ArrayList<>();
        
        int count = 3;
        for (int i = 0; i < count; i++) {
        	SecureRandom secrnd = new SecureRandom("some bytes".getBytes());
        	byte[] buffer = new byte[16];
        	secrnd.nextBytes(buffer);
        	tokens.add(Hex.toHexString(buffer));
        }
        
        // Note, a seeded SecureRandom still produces different values
        
        Assert.assertEquals(count, tokens.stream()
                .peek(tok -> LOG.info(tok))
                .distinct().count());
        
    }
    
    @Test
    public void testDeterministicRandom() throws Exception {
        
        List<String> tokens = new ArrayList<>();
        
        int count = 3;
        for (int i = 0; i < count; i++) {
        	SecureRandom secrnd = new DeterministicRandom("some bytes".getBytes());
        	byte[] buffer = new byte[16];
        	secrnd.nextBytes(buffer);
        	tokens.add(Hex.toHexString(buffer));
        }
        
        // Note, a seeded DeterministicRandom produces idempotent values
        
        Assert.assertEquals(1, tokens.stream()
                .peek(tok -> LOG.info(tok))
                .distinct().count());
        
    }
}
