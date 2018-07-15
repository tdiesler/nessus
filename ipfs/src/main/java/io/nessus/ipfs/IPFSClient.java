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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.AssertState;

public class IPFSClient extends AbstractClient {

    public static final String ENV_IPFS_API_HOST = "AEG_IPFS_API_HOST";
    public static final String ENV_IPFS_API_PORT = "AEG_IPFS_API_PORT";
    
    public static final String ENV_IPFS_GATEWAY_HOST = "AEG_IPFS_GATEWAY_HOST";
    public static final String ENV_IPFS_GATEWAY_PORT = "AEG_IPFS_GATEWAY_PORT";
    
    public static final String ENV_IPFS_WEBUI_HOST = "AEG_IPFS_WEBUI_HOST";
    public static final String ENV_IPFS_WEBUI_PORT = "AEG_IPFS_WEBUI_PORT";
    
    static final Logger LOG = LoggerFactory.getLogger(IPFSClient.class);
    
    final String apiopt;
    
    public IPFSClient() {
        this(10L, TimeUnit.SECONDS);
    }
    
    public IPFSClient(Long timeout, TimeUnit unit) {
        super(timeout, unit);
        
        String host = System.getenv(ENV_IPFS_API_HOST);
        if (host != null) {
            String port = System.getenv(ENV_IPFS_API_PORT);
            port = port != null ? port : "5001";
            apiopt = String.format("--api=/ip4/%s/tcp/%s", host, port);
        } else {
            apiopt = null;
        }
    }

    public String add(String... args) {
        String[] cmd = concat("add", opts(), args); 
        String res = exec(ipfs(), cmd);
        String[] toks = split(res);
        AssertState.assertEquals("added", toks[0]);
        return toks[1];
    }

    public String cat(String... args) {
        String[] cmd = concat("cat", opts(), args); 
        return exec(ipfs(), cmd);
    }

    public String get(String... args) {
        String[] cmd = concat("get", opts(), args); 
        return exec(ipfs(), cmd);
    }

    public String version() {
        String[] cmd = concat("version", opts()); 
        return exec(ipfs(), cmd);
    }

    private String[] opts(String... opts) {
        List<String> list = new ArrayList<>();
        if (apiopt != null) {
            list.add(apiopt);
        }
        if (opts != null) {
            list.addAll(Arrays.asList(opts));
        }
        return list.toArray(new String[list.size()]);
    }

    private String ipfs() {
        return "ipfs";
    }
}
