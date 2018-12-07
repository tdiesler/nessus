package io.nessus.ipfs.impl;

/*-
 * #%L
 * Nessus :: IPFS :: Core
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable.FileWrapper;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.IPFSException;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;
import io.nessus.utils.StreamUtils;
import io.nessus.utils.SystemUtils;

public class DefaultIPFSClient implements IPFSClient {

    static final Logger LOG = LoggerFactory.getLogger(DefaultIPFSClient.class);
    
    private final MultiAddress addr;
    private final IPFS ipfs;
    
    // Executor service for async get operations
    private final ExecutorService executorService;
    
    public DefaultIPFSClient() {
        this(SystemUtils.getenv(ENV_IPFS_JSONRPC_ADDR, "127.0.0.1"), Integer.parseInt(SystemUtils.getenv(ENV_IPFS_JSONRPC_PORT, "5001")));
    }
    
    public DefaultIPFSClient(String host, Integer port) {
        this(new MultiAddress("/ip4/" + host + "/tcp/" + port));
    }

    public DefaultIPFSClient(MultiAddress addr) {
        this.addr = addr;
        
        try {
            ipfs = new IPFS(addr);
        } catch (RuntimeException ex) {
            LOG.error("Cannot connect to: " + addr);
            throw ex;
        }
        
        executorService = Executors.newFixedThreadPool(12, new ThreadFactory() {
            AtomicInteger count = new AtomicInteger();
            public Thread newThread(Runnable run) {
                return new Thread(run, "ipfs-client-" + count.incrementAndGet());
            }
        });
    }

    @Override
    public MultiAddress getAPIAddress() {
        return addr;
    }

    @Override
    public boolean hasConnection() {
        return ipfs != null;
    }

    @Override
    public List<String> add(Path path) throws IOException {
        List<MerkleNode> parts = ipfs().add(new FileWrapper(path.toFile()));
        return parts.stream().map(mn -> mn.hash.toBase58()).collect(Collectors.toList());
    }

    @Override
    public String addSingle(Path path) throws IOException {
        AssertArgument.assertTrue(path.toFile().isFile(), "Not a file: " + path);
        List<String> cids = add(path);
        return cids.size() > 0 ? cids.get(0) : null;
    }

    @Override
    public InputStream cat(String hash) throws IOException {
        Multihash mhash = Multihash.fromBase58(hash);
        return ipfs().catStream(mhash);
    }

    @Override
    public Future<Path> get(String hash, Path outdir) {
        Future<Path> future = executorService.submit(new Callable<Path>() {
            @Override
            public Path call() throws Exception {
                try {
                    Multihash mhash = Multihash.fromBase58(hash);
                    return get(mhash, outdir.resolve(hash));
                } catch (Exception ex) {
                    throw new IPFSException(ex);
                }
            }});
        return future;
    }

    private Path get(Multihash mhash, Path outpath) throws IOException {
        List<MerkleNode> links = ipfs().ls(mhash).get(0).links;
        for (MerkleNode node : links) {
            String name = node.name.get();
            get(node.hash, outpath.resolve(name));
        }
        if (links.isEmpty()) {
            File outfile = outpath.toFile();
            outpath.getParent().toFile().mkdirs();
            try (OutputStream fout = new FileOutputStream(outfile)) {
                InputStream ins = ipfs().catStream(mhash);
                StreamUtils.copyStream(ins, fout);
            }
        }
        return outpath;
    }

    @Override
    public String version() throws IOException {
        return ipfs().version();
    }
    
    private IPFS ipfs() {
        AssertState.assertNotNull(ipfs, "No IPFS connection");
        return ipfs;
    }
}
