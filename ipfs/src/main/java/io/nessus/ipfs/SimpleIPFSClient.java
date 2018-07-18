package io.nessus.ipfs;

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

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.cmd.CmdLineClient;
import io.nessus.utils.AssertState;

public class SimpleIPFSClient extends CmdLineClient implements IPFSClient {

    static final Logger LOG = LoggerFactory.getLogger(SimpleIPFSClient.class);
    
    final String[] opts;
    
    public SimpleIPFSClient() {
        
        String host = System.getenv(ENV_IPFS_API_HOST);
        if (host != null) {
            String port = System.getenv(ENV_IPFS_API_PORT);
            port = port != null ? port : "5001";
            opts = new String[] { String.format("--api=/ip4/%s/tcp/%s", host, port) };
        } else {
            opts = null;
        }
    }

    @Override
    public String add(Path path) {
        String res = exec(concat("add", new Object[] { path }));
        String[] toks = split(res);
        AssertState.assertEquals("added", toks[0]);
        return toks[1];
    }

    @Override
    public String cat(String cid) {
        return exec(concat("cat", new Object[] { cid }));
    }

    @Override
    public String get(String cid, Path outdir) {
        return exec(concat("get", new Object[] { "-o " + outdir, cid }));
    }

    @Override
    public String get(String cid, Path outdir, Long timeout, TimeUnit unit) {
        return exec(concat("get", new Object[] { "-o " + outdir, cid }), timeout, unit);
    }

    @Override
    public String version() {
        return exec(concat("version", null));
    }

    private String concat(String cmd, Object[] args) {
        StringBuffer line = new StringBuffer("ipfs");
        for (String opt : opts != null ? opts : new String[0]) {
            line.append(" " + opt);
        }
        line.append(" " + cmd);
        for (Object arg : args != null ? args : new String[0]) {
            line.append(" " + arg);
        }
        return line.toString();
    }

    private String[] split(String result) {
        return result.split(" ");
    }
}
