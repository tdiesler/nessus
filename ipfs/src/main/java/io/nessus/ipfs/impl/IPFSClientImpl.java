package io.nessus.ipfs.impl;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable.FileWrapper;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.IPFSException;
import io.nessus.utils.StreamUtils;

public class IPFSClientImpl implements IPFSClient {

    static final Logger LOG = LoggerFactory.getLogger(IPFSClientImpl.class);
    
    private final IPFS ipfs;
    
    // Executor service for async get operations
    private final ExecutorService executorService;
    
    public IPFSClientImpl() {
        
        String host = System.getenv(ENV_IPFS_API_HOST);
        String port = System.getenv(ENV_IPFS_API_PORT);
        host = host != null ? host : "127.0.0.1";
        port = port != null ? port : "5001";
        ipfs = new IPFS(new MultiAddress("/ip4/" + host + "/tcp/" + port));
        
        executorService = Executors.newFixedThreadPool(12, new ThreadFactory() {
            AtomicInteger count = new AtomicInteger();
            public Thread newThread(Runnable run) {
                return new Thread(run, "ipfs-pool-" + count.incrementAndGet());
            }
        });
    }

    @Override
    public String add(Path path) throws IOException {
        List<MerkleNode> parts = ipfs.add(new FileWrapper(path.toFile()));
        Multihash result = parts.get(parts.size() - 1).hash;
        return result.toBase58();
    }

    @Override
    public InputStream cat(String hash) throws IOException {
        Multihash mhash = Multihash.fromBase58(hash);
        return ipfs.catStream(mhash);
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
        List<MerkleNode> links = ipfs.ls(mhash).get(0).links;
        for (MerkleNode node : links) {
            String name = node.name.get();
            get(node.hash, outpath.resolve(name));
        }
        if (links.isEmpty()) {
            File outfile = outpath.toFile();
            outpath.getParent().toFile().mkdirs();
            try (OutputStream fout = new FileOutputStream(outfile)) {
                InputStream ins = ipfs.catStream(mhash);
                StreamUtils.copyStream(ins, fout);
            }
        }
        return outpath;
    }

    @Override
    public String version() throws IOException {
        return ipfs.version();
    }
}
