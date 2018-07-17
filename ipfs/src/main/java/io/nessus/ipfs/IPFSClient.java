package io.nessus.ipfs;

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
import java.util.concurrent.Future;

public interface IPFSClient {

    String ENV_IPFS_API_HOST = "IPFS_API_HOST";
    String ENV_IPFS_API_PORT = "IPFS_API_PORT";
    
    String ENV_IPFS_GATEWAY_HOST = "IPFS_GATEWAY_HOST";
    String ENV_IPFS_GATEWAY_PORT = "IPFS_GATEWAY_PORT";

    String add(Path path) throws IOException;

    InputStream cat(String cid) throws IOException;

    Future<Path> get(String cid, Path outdir);

    String version() throws IOException;

}