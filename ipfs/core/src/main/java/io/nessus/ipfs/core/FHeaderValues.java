package io.nessus.ipfs.core;

public class FHeaderValues {
    
    public final String PREFIX;
    public final String VERSION;
    public final String VERSION_STRING;
    public final String FILE_HEADER_END;
    
    FHeaderValues(String prefix, String version) {
        this.PREFIX = prefix;
        this.VERSION = version;
        this.VERSION_STRING = PREFIX + "-Version: " + VERSION;
        this.FILE_HEADER_END = PREFIX.toUpperCase() + "_HEADER_END";
    }
}