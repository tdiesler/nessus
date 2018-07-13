package io.nessus.cipher.utils;

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