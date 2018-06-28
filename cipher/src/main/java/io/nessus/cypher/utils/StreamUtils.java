package io.nessus.cypher.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtils {

    // Hide ctor
    private StreamUtils() {
    }

    public static void copyStream(InputStream ins, OutputStream outs) throws IOException {
        byte[] buffer = new byte[1024];
        int read = ins.read(buffer);
        while (read > 0) {
            outs.write(buffer, 0, read);
            read = ins.read(buffer);
        }
    }

    public static byte[] toBytes(InputStream ins) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copyStream(ins, baos);
        return baos.toByteArray();
    }
}