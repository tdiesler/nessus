package io.nessus.core.ipfs;

import java.io.IOException;
import java.io.InputStream;

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
import java.util.List;
import java.util.concurrent.Future;

import io.ipfs.multiaddr.MultiAddress;

public interface IPFSClient {

    public static final String ENV_IPFS_JSONRPC_ADDR = "IPFS_JSONRPC_ADDR";
    public static final String ENV_IPFS_JSONRPC_PORT = "IPFS_JSONRPC_PORT";
    
    public static final String ENV_IPFS_GATEWAY_ADDR = "IPFS_GATEWAY_ADDR";
    public static final String ENV_IPFS_GATEWAY_PORT = "IPFS_GATEWAY_PORT";
    
    MultiAddress getAPIAddress();
    
    List<String> add(Path path) throws IOException;

    String addSingle(Path path) throws IOException;

    InputStream cat(String cid) throws IOException;

    Future<Path> get(String cid, Path outdir);

    String version() throws IOException;
    
    boolean hasConnection();

}