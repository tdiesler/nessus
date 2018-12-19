package io.nessus.ipfs.jaxrs;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class JAXRSMain {

    public static void main(String[] args) throws Exception {

        JAXRSConfig config = new JAXRSConfig();
        CmdLineParser parser = new CmdLineParser(config);
        
        try {
            parser.parseArgument(args);
        } catch (CmdLineException ex) {
        	helpScreen(parser);
            throw ex;
        }
        
        if (config.help) {
        	helpScreen(parser);
        } else {
            JAXRSApplication.serverStart(config);
        }
    }

    private static void helpScreen(CmdLineParser parser) {
        System.err.println("run-jaxrs [options...]");
        parser.printUsage(System.err);
    }
}
