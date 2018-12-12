package io.nessus.ipfs.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.nessus.ipfs.FHandle;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;

public class FHeader {
    
    public final FHeaderValues fhvals;
    public final Path path;
    public final String owner;
    public final String token;
    public final int length;
    
    private FHeader(FHeaderValues fhvals, Path path, String owner, String token, int length) {
        AssertArgument.assertNotNull(fhvals, "Null fhvals");
        AssertArgument.assertNotNull(path, "Null path");
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(token, "Null token");
        AssertArgument.assertTrue(length != 0, "Invalid length: " + length);
        this.fhvals = fhvals;
        this.path = path;
        this.owner = owner;
        this.token = token;
        this.length = length;
    }

    public static FHeader fromReader(FHeaderValues fhv, Reader rd) throws IOException {
        BufferedReader br = new BufferedReader(rd);
        
        // First line is the version
        String line = br.readLine();
        AssertState.assertTrue(line.startsWith(fhv.VERSION_STRING), "Invalid version: " + line);
        
        String version = line;
        Path path = null;
        String owner = null;
        String token = null;
        
        int length = line.length() + 1;

        // Read more header lines
        while (line != null) {
            line = br.readLine();
            if (line != null) {
                
                length += line.length() + 1;
                
                if (line.startsWith("Path: ")) {
                    path = Paths.get(line.substring(6));
                } else if (line.startsWith("Owner: ")) {
                    owner = line.substring(7);
                } else if (line.startsWith("Token: ")) {
                    token = line.substring(7);
                } else if (line.startsWith(fhv.FILE_HEADER_END)) {
                    line = null;
                }
            }
        }
        
        AssertState.assertEquals(fhv.VERSION_STRING, version);
        
        FHeader fheader = new FHeader(fhv, path, owner, token, length);
        return fheader;
    }

    public static FHeader fromFHandle(FHeaderValues fhv, FHandle fhandle) {
		
		String owner = fhandle.getOwner().getAddress();
        String encToken = fhandle.getSecretToken();
        
        return new FHeader(fhv, fhandle.getPath(), owner, encToken, -1);
	}
    
    void write(Writer wr) {
        PrintWriter pw = new PrintWriter(wr);
        
        // First line is the version
        pw.println(fhvals.VERSION_STRING);
        
        // Second is the location
        pw.println(String.format("Path: %s", path));
        
        // Then comes the owner
        pw.println(String.format("Owner: %s", owner));
        
        // Then comes the encryption token in Base64
        pw.println(String.format("Token: %s", token));
        
        // Then comes an end of header marker
        pw.println(fhvals.FILE_HEADER_END);
    }
    
    public String toString() {
        return String.format("[version=%s, owner=%s, path=%s, token=%s]", fhvals.VERSION, owner, path, token);
    }
}