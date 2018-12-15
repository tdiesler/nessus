package io.nessus.test.cipher;

import org.junit.Test;

import io.nessus.cipher.utils.CipherSanityCheck;

public class CipherSanityTest {

    @Test
    public void testNewKeyEveryTime() throws Exception {
    	
    	CipherSanityCheck.verifyPlatform();
    }
}
