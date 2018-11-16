package io.nessus.ipfs.jaxrs;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

final class Options {

    @Option(name = "--help", help = true)
    boolean help;

    @Argument(metaVar = "cmd", usage = "start/stop the json rpc server")
    String cmd;
}
