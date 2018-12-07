package io.nessus.ipfs.jaxrs;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class JaxrsMain {

    public static void main(String[] args) throws Exception {

        JaxrsConfig config = new JaxrsConfig();
        CmdLineParser parser = new CmdLineParser(config);
        
        try {
            parser.parseArgument(args);
        } catch (CmdLineException ex) {
            parser.printUsage(System.err);
            throw ex;
        }
        
        JaxrsApplication.serverStart(config);
    }
}
