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
import java.util.List;
import java.util.concurrent.Future;

import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;

public interface IPFSClient {

    MultiAddress getAPIAddress();
    
    List<Multihash> add(Path path) throws IOException;

    List<Multihash> add(Path path, boolean hashOnly) throws IOException;
    
    Multihash addSingle(Path path) throws IOException;

    Multihash addSingle(Path path, boolean hashOnly) throws IOException;

    Multihash addSingle(InputStream input) throws IOException;

    Multihash addSingle(InputStream input, boolean hashOnly) throws IOException;

    Multihash addSingle(byte[] bytes) throws IOException;

    Multihash addSingle(byte[] bytes, boolean hashOnly) throws IOException;

    Future<InputStream> cat(Multihash cid) throws IOException;

    Future<Path> get(Multihash cid, Path outdir);

    String version() throws IOException;
    
    boolean hasConnection();

}