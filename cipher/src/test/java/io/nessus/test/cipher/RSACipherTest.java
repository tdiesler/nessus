package io.nessus.test.cipher;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.cipher.RSACipher;
import io.nessus.cipher.utils.RSAUtils;

public class RSACipherTest extends AbstractCipherTest {

    @Test
    public void testNewKeyEveryTime() throws Exception {
        
        List<KeyPair> keys = new ArrayList<>();
        List<String> secmsgs = new ArrayList<>();
        
        int count = 3;
        for (int i = 0; i < count; i++) {
            RSACipher acipher = new RSACipher();
            KeyPair keyPair = RSAUtils.newKeyPair();
            keys.add(keyPair);
            InputStream ins = asStream(text);
            PublicKey pubKey = keyPair.getPublic();
            InputStream secIns = acipher.encrypt(pubKey, ins);
            secmsgs.add(encode(secIns));
        }
        
        // Note, this gives a new encryption token every time
        
        Assert.assertEquals(count, keys.stream()
                .peek(key -> LOG.info(encode(key.getPublic().getEncoded())))
                .distinct().count());
        
        // Note, this gives a new different secret message every time
        
        Assert.assertEquals(count, secmsgs.stream()
                .peek(tok -> LOG.info(tok))
                .distinct().count());
        
        List<String> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            RSACipher acipher = new RSACipher();
            PrivateKey privKey = keys.get(i).getPrivate();
            InputStream secIns = asStream(decode(secmsgs.get(i)));
            InputStream msgIns = acipher.decrypt(privKey, secIns);
            results.add(asString(msgIns));
        }
        
        // Verify that the result is always the same
        
        Assert.assertEquals(1, results.stream()
                .peek(msg -> LOG.info(msg))
                .distinct().count());
        
        Assert.assertEquals(text, results.get(0));
    }

    @Test
    public void testKeyReuse() throws Exception {
        
        KeyPair keyPair = RSAUtils.newKeyPair();
        PublicKey pubKey = keyPair.getPublic();
        String token = encode(pubKey.getEncoded());
        LOG.info(token);
        
        List<String> secmsgs = new ArrayList<>();
        
        int count = 3;
        for (int i = 0; i < count; i++) {
            RSACipher acipher = new RSACipher();
            InputStream ins = asStream(text);
            InputStream secIns = acipher.encrypt(pubKey, ins);
            secmsgs.add(encode(secIns));
        }
        
        // Note, this gives the same secret message every time
        
        Assert.assertEquals(1, secmsgs.stream()
                .peek(tok -> LOG.info(tok))
                .distinct().count());
        
        List<String> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            RSACipher acipher = new RSACipher();
            PrivateKey privKey = keyPair.getPrivate();
            InputStream secIns = asStream(decode(secmsgs.get(i)));
            InputStream msgIns = acipher.decrypt(privKey, secIns);
            results.add(asString(msgIns));
        }
        
        // Verify that the result is always the same
        
        Assert.assertEquals(1, results.stream()
                .peek(msg -> LOG.info(msg))
                .distinct().count());
        
        Assert.assertEquals(text, results.get(0));
    }

    @Test
    public void testDeterministicKey() throws Exception {
        
        KeyPair keyPair = RSAUtils.newKeyPair(addrBob);
        PublicKey pubKey = keyPair.getPublic();
        String token = encode(pubKey.getEncoded());
        LOG.info(token);
        
        Assert.assertEquals("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5SSQgSaIsFYGKxp9uMuwnYi/M1SEx9uq74suJkdbiwUF/Yznz+bu6ZhpimE79lG/g3rn2ptcWXqZ8DcKOucIyCN2JjmRxh5zpRrR9mfV8JYgvYdjLDweUTABidR3w9FkMKrv+1akyz3S/faxy46xl2L10YlC+g3ufLeWrXEjDZckcBJSYSe1KCateAnSSfm/8I733Lr75mBptlPoCdQF5TrfBbkTS7oEcUkk6Mf3ZMpk7Q/QVU7FnK0+JY0kiZruiobSS3WVGCwZaOjKlw/m3PvdW+s/2Ts26Mr1u8HthHk5Bz/GD8SqIfFPvaebYfUpNk6ct8Y6mNNVfKkk+2Fr+QIDAQAB", token);
        Assert.assertEquals(392, token.length());

        // Recreate the public key from its encoded form 
        
        PublicKey pubKeyB = RSAUtils.decodePublicKey(token);
        Assert.assertEquals(pubKey, pubKeyB);
        
        List<String> secmsgs = new ArrayList<>();
        
        int count = 3;
        for (int i = 0; i < count; i++) {
            RSACipher acipher = new RSACipher();
            InputStream ins = asStream(text);
            InputStream secIns = acipher.encrypt(pubKeyB, ins);
            secmsgs.add(encode(secIns));
        }
        
        // Note, this gives the same secret message every time
        
        Assert.assertEquals(1, secmsgs.stream()
                .peek(tok -> LOG.info(tok))
                .distinct().count());
        
        List<String> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            RSACipher acipher = new RSACipher();
            PrivateKey privKey = keyPair.getPrivate();
            InputStream secIns = asStream(decode(secmsgs.get(i)));
            InputStream msgIns = acipher.decrypt(privKey, secIns);
            results.add(asString(msgIns));
        }
        
        // Verify that the result is always the same
        
        Assert.assertEquals(1, results.stream()
                .peek(msg -> LOG.info(msg))
                .distinct().count());
        
        Assert.assertEquals(text, results.get(0));
    }

    @Test
    public void testKeyStrenght() throws Exception {
        
        KeyPair keyPair = RSAUtils.newKeyPair(addrBob, 1024);
        PublicKey pubKey = keyPair.getPublic();
        String token = encode(pubKey.getEncoded());
        LOG.info("{}", pubKey);
        
        Assert.assertEquals(216, token.length());
    }
}
