package io.nessus.ipfs.client;

import java.io.ByteArrayOutputStream;

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
import io.ipfs.api.IPFS.Config;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable.ByteArrayWrapper;
import io.ipfs.api.NamedStreamable.FileWrapper;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;
import io.nessus.utils.StreamUtils;

public class DefaultIPFSClient implements IPFSClient {

    static final Logger LOG = LoggerFactory.getLogger(DefaultIPFSClient.class);
    
    private final MultiAddress addr;
    
    private IPFS ipfs;
    
    // Executor service for async get operations
    private final ExecutorService executorService;
    
    public DefaultIPFSClient(String host, Integer port) {
        this(new MultiAddress("/ip4/" + host + "/tcp/" + port));
    }

    public DefaultIPFSClient(MultiAddress addr) {
        this.addr = addr;
        
        executorService = Executors.newFixedThreadPool(12, new ThreadFactory() {
            AtomicInteger count = new AtomicInteger();
            public Thread newThread(Runnable run) {
                return new Thread(run, "ipfs-client-" + count.incrementAndGet());
            }
        });
    }

    @Override
    public IPFSClient connect() {
        try {
            ipfs = new IPFS(addr);
            return this;
        } catch (RuntimeException ex) {
            throw new IPFSException(ex);
        }
    }

    public IPFS getIpfs() {
        AssertState.assertNotNull(ipfs, "No IPFS connection");
        return ipfs;
    }
    
	@Override
	public Config getIpfsConfig() {
		return getIpfs().config;
	}

	@Override
	public String getPeerId() throws IOException {
		return getIpfsConfig().get("Identity.PeerID");
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
    public List<Multihash> add(Path path) throws IOException {
        return add(path, false);
    }

    @Override
    public List<Multihash> add(Path path, boolean hashOnly) throws IOException {
        List<MerkleNode> parts = getIpfs().add(new FileWrapper(path.toFile()), false, hashOnly);
        AssertState.assertTrue(parts.size() > 0, "No content added");
        return parts.stream().map(mn -> mn.hash).collect(Collectors.toList());
    }

    @Override
    public Multihash addSingle(Path path) throws IOException {
        return addSingle(path, false);
    }

    @Override
    public Multihash addSingle(Path path, boolean hashOnly) throws IOException {
        AssertArgument.assertTrue(path.toFile().isFile(), "Not a file: " + path);
        List<Multihash> cids = add(path, hashOnly);
        AssertState.assertTrue(cids.size() > 0, "No content added");
        return cids.get(0);
    }

    @Override
    public Multihash addSingle(InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        return addSingle(baos.toByteArray());
    }

    @Override
    public Multihash addSingle(InputStream input, boolean hashOnly) throws IOException {
        return addSingle(input, false);
    }

    @Override
    public Multihash addSingle(byte[] bytes) throws IOException {
        return addSingle(bytes, false);
    }

    @Override
    public Multihash addSingle(byte[] bytes, boolean hashOnly) throws IOException {
        List<MerkleNode> parts = getIpfs().add(new ByteArrayWrapper(bytes), false, hashOnly);
        AssertState.assertTrue(parts.size() > 0, "No content added");
        Multihash cid = parts.stream().map(mn -> mn.hash).findFirst().get();
        return cid;
    }

    @Override
    public Future<InputStream> cat(Multihash cid) throws IOException {
    	
        Callable<InputStream> call = new Callable<InputStream>() {
        	
            @Override
            public InputStream call() throws Exception {
                try {
                    return getIpfs().catStream(cid);
                } catch (Exception ex) {
                    throw new IPFSException(ex);
                }
            }
        };
        
		Future<InputStream> future = executorService.submit(call);
        return future;
    }

    @Override
    public Future<Path> get(Multihash cid, Path outdir) {
    	
        Callable<Path> call = new Callable<Path>() {
        	
            @Override
            public Path call() throws Exception {
                try {
                    return getInternal(cid, outdir.resolve(cid.toBase58()));
                } catch (Exception ex) {
                    throw new IPFSException(ex);
                }
            }
            
            Path getInternal(Multihash cid, Path outpath) throws IOException {
                List<MerkleNode> links = getIpfs().ls(cid).get(0).links;
                for (MerkleNode node : links) {
                    String name = node.name.get();
                    getInternal(node.hash, outpath.resolve(name));
                }
                if (links.isEmpty()) {
                    File outfile = outpath.toFile();
                    outpath.getParent().toFile().mkdirs();
                    try (OutputStream fout = new FileOutputStream(outfile)) {
                        InputStream ins = getIpfs().catStream(cid);
                        StreamUtils.copyStream(ins, fout);
                    }
                }
                return outpath;
            }
        };
        
		Future<Path> future = executorService.submit(call);
        return future;
    }


    @Override
    public String version() throws IOException {
        return getIpfs().version();
    }
}
