package io.nessus.ipfs;

@SuppressWarnings("serial")
public class TimeoutException extends RuntimeException {

    public TimeoutException(String message) {
        super(message);
    }
}
