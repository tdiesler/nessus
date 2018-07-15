package io.nessus.cipher.utils;

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

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import io.nessus.AssertState;

@SuppressWarnings("serial") 
public class DeterministicRandom extends SecureRandom {
    
    final byte[] bytes;
    final AtomicBoolean used = new AtomicBoolean();
    
    public DeterministicRandom(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public void nextBytes(byte[] buffer) {
        AssertState.assertFalse(used.getAndSet(true), "Generator can only be used once");
        AssertState.assertTrue(buffer.length <= bytes.length, "Insufficient number of bytes");
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = bytes[i];
        }
    }
}
