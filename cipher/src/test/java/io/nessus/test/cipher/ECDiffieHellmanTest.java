package io.nessus.test.cipher;

/*-
 * #%L
 * Nessus :: Cipher
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

import java.security.KeyPair;

import org.junit.Assert;
import org.junit.Test;

import io.nessus.cipher.ECDH;

public class ECDiffieHellmanTest {

    @Test
    public void testECDH() throws Exception {

        ECDH ecdh = new ECDH();

        KeyPair pairA = ecdh.generateKeyPair();
        KeyPair pairB = ecdh.generateKeyPair();
        byte[] prvA = ecdh.savePrivateKey(pairA.getPrivate());
        byte[] pubA = ecdh.savePublicKey(pairA.getPublic());
        byte[] prvB = ecdh.savePrivateKey(pairB.getPrivate());
        byte[] pubB = ecdh.savePublicKey(pairB.getPublic());

        byte[] secretA = ecdh.generateSecret(prvA, pubB);
        byte[] secretB = ecdh.generateSecret(prvB, pubA);
        Assert.assertArrayEquals(secretA, secretB);
    }
}
