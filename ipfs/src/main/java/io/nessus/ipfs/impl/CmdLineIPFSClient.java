package io.nessus.ipfs.impl;

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
import io.nessus.cmd.CmdLineException;
import io.nessus.cmd.CmdLineTimeoutException;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.IPFSException;
import io.nessus.ipfs.IPFSTimeoutException;
import io.nessus.ipfs.MerkleNotFoundException;
import io.nessus.utils.AssertState;

public class CmdLineIPFSClient extends CmdLineClient implements IPFSClient {

    static final Logger LOG = LoggerFactory.getLogger(CmdLineIPFSClient.class);
    
    final String[] opts;
    
    public CmdLineIPFSClient() {
        
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
    public String add(Path path, boolean recursive) {
        String cmd = recursive ? "add -r" : "add";
        String res = execIPFS(concat(cmd, new Object[] { path }));
        String[] toks = split(res);
        AssertState.assertEquals("added", toks[0]);
        return toks[1];
    }

    @Override
    public String cat(String cid) {
        return execIPFS(concat("cat", new Object[] { cid }));
    }

    @Override
    public String get(String cid, Path outdir) {
        return execIPFS(concat("get", new Object[] { "-o " + outdir, cid }));
    }

    @Override
    public String get(String cid, Path outdir, Long timeout, TimeUnit unit) {
        return execIPFS(concat("get", new Object[] { "-o " + outdir, cid }), timeout, unit);
    }

    @Override
    public String version() {
        return execIPFS(concat("version", null));
    }

    private String execIPFS(String cmdLine) {
        return execIPFS(cmdLine, null, null);
    }

    private String execIPFS(String cmdLine, Long timeout, TimeUnit unit) {
        try {
            
            return exec(cmdLine, timeout, unit);
            
        } catch (CmdLineTimeoutException ex) {
            
            throw new IPFSTimeoutException(cmdLine, ex);
            
        } catch (CmdLineException ex) {
            
            Throwable cause = ex.getCause();
            if (cause != null) {
                if ("Error: merkledag: not found".equals(cause.getMessage())) {
                    cause = new MerkleNotFoundException(cmdLine);
                }
            }
            
            throw new IPFSException(cmdLine, cause);
            
        }
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
