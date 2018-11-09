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
    
    private MultiAddress addr;
    private IPFS ipfs;
    
    // Executor service for async get operations
    private final ExecutorService executorService;
    
    public DefaultIPFSClient() {
        this(null, null);
    }
    
    public DefaultIPFSClient(String host, Integer port) {
        
        if (host == null) {
            String envvar = SystemUtils.getenv(ENV_IPFS_API_HOST, "127.0.0.1");
            host = envvar;
        }
            
        if (port == null) {
            String envvar = SystemUtils.getenv(ENV_IPFS_API_PORT, "5001");
            port = Integer.parseInt(envvar);
        }
        
        addr = new MultiAddress("/ip4/" + host + "/tcp/" + port);
        try {
            ipfs = new IPFS(addr);
        } catch (RuntimeException ex) {
            LOG.error("Cannot connect to: " + addr);
        }
        
        executorService = Executors.newFixedThreadPool(12, new ThreadFactory() {
            AtomicInteger count = new AtomicInteger();
            public Thread newThread(Runnable run) {
                return new Thread(run, "ipfs-pool-" + count.incrementAndGet());
            }
        });
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
