package io.nessus.test.bitcoin;

/*-
 * #%L
 * Nessus :: Bitcoin
 * %%
 * Copyright (C) 2018 Nessus
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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

