package io.nessus.test.bitcoin;

import java.security.SecureRandom;
import java.util.List;

import org.bitcoinj.crypto.LinuxSecureRandom;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.wallet.DeterministicSeed;
import org.junit.Assert;
import org.junit.Test;

import io.nessus.bitcoin.AbstractBitcoinTest;


public class MnemonicTest extends AbstractBitcoinTest {
    
    static final SecureRandom secureRandom = new SecureRandom();

    static {
        new LinuxSecureRandom();
    }
    
    @Test
    public void testMnemonicCode () throws Exception {
        
        MnemonicCode mnemonic = new MnemonicCode();
        byte[] seed = secureRandom.generateSeed(16);
        List<String> words = mnemonic.toMnemonic(seed);
        LOG.info("Words: {}", words.toString().replace(", ", " "));
        Assert.assertEquals(words.toString(), 12, words.size());

        DeterministicSeed dets = new DeterministicSeed(seed, words, System.currentTimeMillis());
        String hexString = dets.toHexString();
        LOG.info("Seed: {}", hexString);
    }
}

