package io.nessus.ipfs.core;

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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import io.ipfs.multihash.Multihash;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.Config;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.FHandle.FHBuilder;
import io.nessus.ipfs.FHandle.FHReference;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.IPFSException;
import io.nessus.ipfs.IPFSNotFoundException;
import io.nessus.ipfs.IPFSTimeoutException;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;

public class FHandleManager extends AbstractHandleManager {
	
	FHandleManager(DefaultContentManager cntmgr) {
		super(cntmgr);
	}
	
	public FHandle addIpfsContent(FHandle fhandle, boolean dryRun) throws IOException {
        
		IPFSClient ipfsClient = cntmgr.getIPFSClient();
        
        Path auxPath = fhandle.getFilePath();
        AssertState.assertTrue(auxPath.toFile().exists(), "Encrypted content does not exists: " + auxPath);
        
        List<Multihash> cids = ipfsClient.add(auxPath, dryRun);
        AssertState.assertTrue(cids.size() > 0, "No ipfs content ids");
        
        Multihash cid = cids.get(cids.size() - 1);
        FHandle fhres = new FHBuilder(fhandle).cid(cid).build();
        
        return fhres;
	}

    public FHandle getIpfsContent(FHandle fhandle, long timeout) throws IOException, IPFSTimeoutException {
        AssertArgument.assertNotNull(fhandle, "Null fhandle");
        AssertArgument.assertNotNull(fhandle.getOwner(), "Null owner");
        AssertArgument.assertNotNull(fhandle.getCid(), "Null cid");
        
        Address owner = fhandle.getOwner();
        Multihash cid = fhandle.getCid();
        
        Path cryptPath = cntmgr.getCryptPath(owner);
        Path rootPath = cryptPath.resolve(cid.toBase58());
        
        // Fetch the content from IPFS
        
        if (!rootPath.toFile().exists()) {
            
            long before = System.currentTimeMillis();
            
            try {
                
            	IPFSClient ipfsClient = cntmgr.getIPFSClient();
                Future<Path> future = ipfsClient.get(cid, cryptPath);
                Path resPath = future.get(timeout, TimeUnit.MILLISECONDS);
                
                AssertState.assertEquals(rootPath, resPath);
                
            } catch (InterruptedException | ExecutionException ex) {
                
                Throwable cause = ex.getCause();
                if (cause instanceof IPFSException) 
                    throw (IPFSException)cause;
                else 
                    throw new IPFSException(ex);
                
            } catch (TimeoutException ex) {
                
                throw new IPFSTimeoutException(ex);
                
            } finally {
                
                long elapsed = System.currentTimeMillis() - before;
                fhandle = new FHBuilder(fhandle)
                        .elapsed(elapsed)
                        .build();
                
                IPFSCache ipfsCache = cntmgr.getIPFSCache();
                ipfsCache.put(fhandle, FHandle.class);
            }
        }
        
        if (fhandle.getURL() == null) {
            URL furl = rootPath.toUri().toURL();
            fhandle = new FHBuilder(fhandle).url(furl).build();
        }
        
        File rootFile = rootPath.toFile();
        AssertState.assertTrue(rootFile.exists(), "Cannot find IPFS content at: " + rootFile);
        
        if (rootFile.isDirectory()) {
            fhandle = createIPFSFileTree(fhandle);
        } else {
            fhandle = createFromFileHeader(null, fhandle);
        }
        
        FHandle fhres = new FHBuilder(fhandle)
                .available(true)
                .build();
        
        LOG.info("IPFS found: {}", fhres.toString(true));

        IPFSCache ipfsCache = cntmgr.getIPFSCache();
        ipfsCache.put(fhres, FHandle.class);
        
        return fhres;
    }

    public List<FHandle> findIpfsContentAsync(List<FHandle> fhandles, long timeout) throws IOException {
        
    	IPFSCache ipfsCache = cntmgr.getIPFSCache();
    	
        synchronized (ipfsCache) {
            for (FHandle fhaux : fhandles) {
            	Multihash cid = fhaux.getCid();
                FHandle fhc = ipfsCache.get(cid, FHandle.class);
                if (fhc == null) {
                    fhc = new FHBuilder(fhaux).elapsed(0L).build();
                    LOG.info("IPFS submit: {}", fhc);
                    ipfsCache.put(fhc, FHandle.class);
                }
            }
        }
        
        Future<List<FHandle>> future = executorService.submit(new Callable<List<FHandle>>() {

            @Override
            public List<FHandle> call() throws Exception {
                
                List<FHandle> missing = getMissingFHandles(fhandles);
                
                while(!missing.isEmpty()) {
                    for (FHandle fh : missing) {
                        if (fh.setScheduled(true)) {
                            AsyncGetCallable callable = new AsyncGetCallable(fh, timeout);
                            executorService.submit(callable);
                        }
                    }
                    Thread.sleep(500L);
                    missing = getMissingFHandles(fhandles);
                }
                
                return getCurrentFHandles(fhandles);
            }
        });
        
        List<FHandle> results;
        try {
            results = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException ex) {
            throw new IllegalStateException(ex);
        } catch (TimeoutException ex) {
            results = getCurrentFHandles(fhandles);
        }
        
        return results;
    }

    private FHandle createIPFSFileTree(FHandle fhandle) throws IOException {
        
        Stack<FHandle> fhstack = new Stack<>();
        
        // Find the root node path by reading the 
        // file header of the first encrypted file we find
        
        Path rootPath = fhandle.getFilePath();
        File rootFile = rootPath.toFile();
        AssertState.assertTrue(rootFile.exists(), "Cannot find IPFS content at: " + rootFile);
        
        FHReference fhref = new FHReference();
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                
                URL furl = file.toUri().toURL();
                FHandle fhaux = new FHBuilder(fhandle)
                        .url(furl)
                        .build();
                
                FHandle fhres = createFromFileHeader(null, fhaux);
                
                Path path = fhres.getPath().getName(0);
                furl = rootFile.toURI().toURL();

                fhres = new FHBuilder(fhres)
                        .path(path)
                        .url(furl)
                        .build();
                
                fhref.setFHandle(fhres);
                
                return FileVisitResult.TERMINATE;
            }
        });
        
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                FHandle parent = !fhstack.isEmpty() ? fhstack.peek() : null;
                FHandle fhandle = createFileHandle(parent, dir);
                fhstack.push(fhandle);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                fhstack.pop();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                FHandle parent = !fhstack.isEmpty() ? fhstack.peek() : null;
                createFileHandle(parent, file);
                return FileVisitResult.CONTINUE;
            }
            
            FHandle createFileHandle(FHandle parent, Path fullPath) throws IOException {
                
                FHandle fhres;
                if (fullPath.toFile().isDirectory()) {
                    
                    if (parent == null) {
                        
                        // Root handle                         
                        fhres = fhref.getFHandle();
                        
                    } else {
                        
                        // Sub directory handles
                        
                        FHandle fhroot = fhref.getFHandle();
                        Path relPath = rootPath.relativize(fullPath);
                        relPath = fhroot.getPath().resolve(relPath);
                        URL furl = fullPath.toUri().toURL();
                        
                        fhres = new FHBuilder(fhandle)
                                .parent(parent)
                                .path(relPath)
                                .url(furl)
                                .build();
                    }
                    
                } else {
                    
                    URL furl = fullPath.toUri().toURL();
                    FHandle fhaux = new FHBuilder(fhandle)
                            .url(furl)
                            .build();

                    fhres = createFromFileHeader(parent, fhaux);
                }
                
                return fhres;
            }
        });
        
        FHandle fhres = fhref.getFHandle();
        AssertState.assertNotNull(fhres, "Cannot obtain fhandle for: " + rootFile);
        
        return fhres;
    }

    private FHandle createFromFileHeader(FHandle parent, FHandle fhandle) throws IOException {
        AssertArgument.assertNotNull(fhandle, "Null fhandle");
        
        Path fullPath = fhandle.getFilePath();
        AssertState.assertTrue(fullPath.toFile().isFile(), "Cannot find IPFS content at: " + fullPath);
        
        try (FileReader fr = new FileReader(fullPath.toFile())) {

        	FHeaderValues fhvals = cntmgr.getFHeaderValues();
            FHeader header = FHeader.fromReader(fhvals, fr);
            Address owner = assertAddress(header.owner);
            String encToken = header.token;
            Path path = header.path;
            
            AssertState.assertEquals(fhandle.getOwner(), owner, "Unexpected owner: " + owner);
            
            boolean available = parent != null ? parent.isAvailable() : false;
            
            FHandle fhres = new FHBuilder(fhandle)
                    .secretToken(encToken)
                    .available(available)
                    .parent(parent)
                    .owner(owner)
                    .path(path)
                    .build();
            
            return fhres;
        }
    }
    
    private List<FHandle> getMissingFHandles(List<FHandle> fhandles) {
    	IPFSCache ipfsCache = cntmgr.getIPFSCache();
        synchronized (ipfsCache) {
            List<FHandle> result = getCurrentFHandles(fhandles).stream()
                    .filter(fh -> fh.isMissing())
                    .collect(Collectors.toList());
            return result;
        }
    }
    
    private List<FHandle> getCurrentFHandles(List<FHandle> fhandles) {
    	IPFSCache ipfsCache = cntmgr.getIPFSCache();
        synchronized (ipfsCache) {
            List<FHandle> result = fhandles.stream()
                .map(fh -> fh.getCid())
                .map(cid -> ipfsCache.get(cid, FHandle.class))
                .collect(Collectors.toList());
            return result;
        }
    }
    
    class AsyncGetCallable implements Callable<FHandle> {
        
        final long timeout;
        final FHandle fhandle;
        
        AsyncGetCallable(FHandle fhandle, long timeout) {
            AssertArgument.assertNotNull(fhandle, "Null fhandle");
            AssertArgument.assertTrue(fhandle.isScheduled(), "Not scheduled");
            this.timeout = timeout;
            this.fhandle = fhandle;
        }

        @Override
        public FHandle call() throws Exception {
            
            int attempt = fhandle.getAttempt() + 1;
            
            FHandle fhaux = new FHBuilder(fhandle)
                    .attempt(attempt)
                    .build();
            
            IPFSCache ipfsCache = cntmgr.getIPFSCache();
            ipfsCache.put(fhaux, FHandle.class);
            
            LOG.info("{}: {}", logPrefix("attempt", attempt),  fhaux);
            
            try {
                
                fhaux = getIpfsContent(fhaux, timeout);
                
            } catch (Exception ex) {
                
                fhaux = processException(fhaux, ex);
                
            } finally {
                
                fhaux.setScheduled(false);
                ipfsCache.put(fhaux, FHandle.class);
            }

            return fhaux;
        }
        
        private FHandle processException(FHandle fhandle, Exception ex) throws InterruptedException {
            
            IPFSCache ipfsCache = cntmgr.getIPFSCache();
            Config config = cntmgr.getConfig();
            
            Multihash cid = fhandle.getCid();
            FHandle fhres = ipfsCache.get(cid, FHandle.class);
            int attempt = fhres.getAttempt();
            
            if (ex instanceof IPFSTimeoutException) {
                
                if (config.getIpfsAttempts() <= attempt) {
                    fhres = new FHBuilder(fhres)
                            .expired(true)
                            .build();
                }
                
                LOG.info("{}: {}", logPrefix("timeout", attempt),  fhres);
            }
            
            else if (ex instanceof IPFSNotFoundException) {
                
                fhres = new FHBuilder(fhres)
                        .expired(true)
                        .build();
                
                LOG.warn("{}: {}", logPrefix("no merkle", attempt),  fhres);
            }
            
            else {
                
                fhres = new FHBuilder(fhres)
                        .expired(true)
                        .build();
                
                LOG.error(logPrefix("error", attempt) + ": " + fhres, ex);
            }
            
            return fhres;
        }

        private String logPrefix(String action, int attempt) {
            Config config = cntmgr.getConfig();
            int ipfsAttempts = config.getIpfsAttempts();
            String trdName = Thread.currentThread().getName();
            return String.format("IPFS %s [%s] [%d/%d]", action, trdName, attempt, ipfsAttempts);
        }
    }
}