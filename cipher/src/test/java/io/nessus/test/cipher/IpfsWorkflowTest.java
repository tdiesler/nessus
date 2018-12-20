package io.nessus.test.cipher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.cipher.AESCipher;
import io.nessus.cipher.RSACipher;
import io.nessus.cipher.utils.AESUtils;
import io.nessus.cipher.utils.RSAUtils;
import io.nessus.utils.StreamUtils;

/**
 * Encryption
 * ----------
 * 
 * An RSA key pair is derived from the Bob's private key.
 * The public RSA key is registered on the blockchain.
 * The RSA key pair is indempotent for the same user.
 * 
 * An AES secret is derived from Bob's private key and the content id.
 * The AES secret is encrypted with Mary's public RSA key, which results in an encryption token.
 * The encryption token is recorded in the ipfs file header.
 * An AES initialization vector (IV) is derived from Bob's private key and the content id.
 * The file content as encrypted with the AES secret and the deterministic IV.
 * The encrypted file (which consists of header and body) is added to IPFS.
 * The resulting IPFS content id (CID) is recorded on the blockchain.
 * The CID for the encrypted content is indempotent for the same user and content.
 * 
 * Decryption
 * ----------
 * 
 * Mary obtains an IPFS content id from her unspent transaction outputs (UTXO).
 * An IPFS get operation fetches the encrypted file (i.e. header + body)
 * Mary obtains the encryption token from the file header.
 * The encryption token is decrypted with the Mary's private RSA key, which results in the AES secret.
 * Mary decrypts the file body with the so obtained AES secret.
 * The plain content is stored in Mary's workspace.
 * 
 * KEEP IN SYNC WITH ContentManager
 */
public class IpfsWorkflowTest extends AbstractCipherTest {

    @Test
    public void testWorkflow() throws Exception {
    	
        RSACipher rsa = new RSACipher();
        AESCipher aes = new AESCipher();
        
        //
        // Encryption
        //
        
    	// An RSA key pair is derived from the Bob's blockchain private key.
    	
        KeyPair rsaBob = RSAUtils.newKeyPair(addrBob);
        PublicKey pubBob = rsaBob.getPublic();
		String token = RSAUtils.encodeKey(pubBob);
        LOG.info(token);
        
        assertEquals("MIIBIjAN...+QIDAQAB", token);
        Assert.assertEquals(392, token.length());

        // An AES secret is derived from Bob's blockchain private key and the content id.
        
        SecretKey aesSecret = AESUtils.newSecretKey(addrBob, cid);
        token = AESUtils.encodeKey(aesSecret);
        LOG.info(token);
        
        Assert.assertEquals("su/CDryLQR9yAJZ+P8TDrQ==", token);
        
        // The AES secret is encrypted with Mary's public RSA key, which results in an encryption token.
        
        KeyPair rsaMary = RSAUtils.newKeyPair(addrMary);
        PublicKey pubMary = rsaMary.getPublic();
        String encToken = encode(rsa.encrypt(pubMary, aesSecret.getEncoded()));
        LOG.info(encToken);
        
        assertEquals("Wh69ziWl...gc/o2A==", encToken);
        Assert.assertEquals(344, encToken.length());
        
        // The file content as encrypted with the AES secret.
        
        byte[] iv = AESUtils.getIV(addrBob, cid);
        InputStream input = new ByteArrayInputStream(text.getBytes());
        String encContent = encode(aes.encrypt(aesSecret, iv, input, null));

        assertEquals("AAAADIdy...FSaSdEnF", encContent);
        Assert.assertEquals(100, encContent.length());
        
        //
        // Decryption
        //
        
        // The encryption token is decrypted with the Mary's private RSA key, which results in the AES secret.
        
        PrivateKey prvMary = rsaMary.getPrivate();
        aesSecret = AESUtils.decodeSecretKey(rsa.decrypt(prvMary, decode(encToken)));
        token = AESUtils.encodeKey(aesSecret);
        LOG.info(token);
        
        Assert.assertEquals("su/CDryLQR9yAJZ+P8TDrQ==", token);
        
        // Mary decrypts the file body with the so obtained AES secret.
        
        input = new ByteArrayInputStream(decode(encContent));
        InputStream decrypt = aes.decrypt(aesSecret, input, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamUtils.copyStream(decrypt, baos);
        String result = new String(baos.toByteArray());
        LOG.info(result);
        
		Assert.assertEquals(text, result);
    }

	private void assertEquals(String expHeadTail, String token) {
		int length = token.length();
        String head = token.substring(0, 8);
        String tail = token.substring(length - 8, length);
        Assert.assertEquals(expHeadTail, head + "..." + tail);
	}
}
