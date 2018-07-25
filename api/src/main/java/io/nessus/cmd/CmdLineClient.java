package io.nessus.cmd;

/*-
 * #%L
 * Nessus :: IPFS
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

public class CmdLineClient {

    final Logger LOG = LoggerFactory.getLogger(getClass());

    static final String[] PATHS = new String[] { "/usr/bin/", "/usr/local/bin/" };
    
    public String exec(String cmdLine) {
        return exec(cmdLine, null, null);
    }
    
    public String exec(String cmdLine, Long timeout, TimeUnit unit) throws TimeoutException {

        String result = null;

        try {

            String cmd = cmdLine.split(" ")[0];
            
            // Find the full path to the executable 
            if (!cmd.startsWith("/")) {
                for (String aux : PATHS) {
                    if (Paths.get(aux, cmd).toFile().exists()) {
                        cmdLine = aux + cmdLine;
                        break;
                    }
                }
            }

            LOG.debug("> {}", cmdLine);

            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec(cmdLine);

            StreamGobbler stderr = new StreamGobbler(proc.getErrorStream());
            StreamGobbler stdout = new StreamGobbler(proc.getInputStream());

            stderr.start();
            stdout.start();

            if (timeout != null) {
                if (!proc.waitFor(timeout, unit)) {
                    String errmsg = "Timeout executing: " + cmdLine;
                    LOG.error(errmsg);
                    throw new TimeoutException(errmsg);
                }
            } else {
                proc.waitFor();
            }
            
            if (proc.exitValue() == 0) {
                result = stdout.result();
                LOG.debug(result);
            } else {
                CmdLineException cause = null;
                if (stderr.length() > 0) {
                    String errmsg = stderr.result();
                    cause = new CmdLineException(errmsg);
                }
                String errmsg = "ERROR executing: " + cmdLine;
                if (cause != null) errmsg += ", caused by: " + cause.getMessage();
                LOG.error(errmsg);
                throw new CmdLineException(errmsg, cause);
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        return result;
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
