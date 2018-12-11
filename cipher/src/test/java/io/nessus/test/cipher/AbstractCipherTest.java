package io.nessus.test.cipher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ipfs.multihash.Multihash;
import io.nessus.AbstractAddress;
import io.nessus.Wallet.Address;
import io.nessus.utils.StreamUtils;

public class AbstractCipherTest {

    final Logger LOG = LoggerFactory.getLogger(getClass());
    
    // ipfs add -n ipfs/core/src/test/resources/userfile.txt
    String text = "The quick brown fox jumps over the lazy dog";
    Multihash cid = Multihash.fromBase58("QmSXpkKkXMnsqWEnWYAokFuyQUxQ1pR1FgbnpyKNrJdPwV");
    
    InputStream asStream(String text) throws IOException {
        return asStream(text.getBytes());
    }
    
    InputStream asStream(byte[] bytes) throws IOException {
        return new ByteArrayInputStream(bytes);
    }
    
    String asString(InputStream ins) throws IOException {
        return new String(StreamUtils.toBytes(ins));
    }
    
    String encode(InputStream input) throws IOException {
        return encode (StreamUtils.toBytes(input));
    }
    
    String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
    
    byte[] decode(String encmsg) {
        return Base64.getDecoder().decode(encmsg);
    }
    
    Address addrBob = new AbstractAddress("n3ha6rJa8ZS7B4v4vwNWn8CnLHfUYXW1XE") {
        public String getPrivKey() {
            return "cVfiZLCWbCm3SWoBAToaCoMuYJJjEw5cR6ifuWQY1a5wadXynGC2";
        }
    };
}
