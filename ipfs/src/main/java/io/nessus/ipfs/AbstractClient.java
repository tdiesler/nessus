package io.nessus.ipfs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.ipfs.TimeoutException;

class AbstractClient {

    final Logger LOG = LoggerFactory.getLogger(getClass());

    static final String[] PATHS = new String[] { "/usr/bin/", "/usr/local/bin/" };
    
    private final Long timeout;
    private final TimeUnit unit;

    protected AbstractClient(Long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }

    public String exec(String cmd, String[] args) throws TimeoutException {

        String result = null;

        try {

            StringBuffer cmdLine = new StringBuffer(cmd);
            if (args != null) {
                for (String arg : args) {
                    cmdLine.append(" " + arg);
                }
            }

            // Find the full path to the executable 
            String path = null;
            for (String aux : PATHS) {
                if (Paths.get(aux, cmd).toFile().exists()) {
                    path = aux;
                    break;
                }
            }

            LOG.info("> {}", cmdLine);

            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec(path + cmdLine);

            StreamGobbler stderr = new StreamGobbler(proc.getErrorStream());
            StreamGobbler stdout = new StreamGobbler(proc.getInputStream());

            stderr.start();
            stdout.start();

            if (timeout != null) {
                if (!proc.waitFor(timeout, unit)) {
                    throw new TimeoutException("Timeout executing: " + cmdLine);
                }
            } else {
                proc.waitFor();
            }
            
            if (proc.exitValue() == 0) {
                result = stdout.result();
                LOG.info(result);
            } else {
                if (stderr.length() > 0) {
                    LOG.error(stderr.result());
                }
                throw new IllegalStateException("ERROR executing: " + cmdLine);
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        return result;
    }

    protected String[] concat(String cmd, String[] opts, String... args) {
        opts = opts != null ? opts : new String[0];
        args = args != null ? args : new String[0];
        String[] toks = new String[1 + opts.length + args.length];
        System.arraycopy(opts, 0, toks, 0, opts.length);
        toks[opts.length] = cmd;
        System.arraycopy(args, 0, toks, opts.length + 1, args.length);
        return toks;
    }

    protected String[] split(String result) {
        return result.split(" ");
    }

    static class StreamGobbler extends Thread {

        final InputStream is;
        final StringWriter sw;

        StreamGobbler(InputStream is) {
            this.is = is;
            this.sw = new StringWriter();
        }

        public int length() {
            return sw.toString().length();
        }

        public String result() {
            return length() > 0 ? sw.toString().trim() : null;
        }

        public void run() {
            try {
                String line = null;
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                PrintWriter pw = new PrintWriter(sw);
                while ((line = br.readLine()) != null) {
                    pw.println(line);
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }
}
