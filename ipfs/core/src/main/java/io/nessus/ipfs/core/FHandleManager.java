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

import io.ipfs.multihash.Multihash;
import io.nessus.Tx;
import io.nessus.TxOutput;
import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.ContentManagerConfig;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.FHandle.FHBuilder;
import io.nessus.ipfs.FHandle.FHReference;
import io.nessus.ipfs.client.IPFSClient;
import io.nessus.ipfs.client.IPFSException;
import io.nessus.ipfs.client.IPFSNotFoundException;
import io.nessus.ipfs.client.IPFSTimeoutException;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;

public class FHandleManager extends AbstractHandleManager<FHandle> {
	
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
        
        IPFSCache ipfsCache = cntmgr.getIPFSCache();
        FHandle fhres = ipfsCache.get(cid, FHandle.class);
        if (fhres.isAvailable()) return fhres;
        
        // Fetch the content from IPFS
        
        int attempt = fhres.getAttempt();
        LOG.info("{}: {}", logPrefix("attempt", attempt),  fhres);
        
        long before = System.currentTimeMillis();
        
        try {
            
            Path cryptPath = cntmgr.getCryptPath(owner);
        	IPFSClient ipfsClient = cntmgr.getIPFSClient();
            Future<Path> future = ipfsClient.get(cid, cryptPath);
            Path resPath = future.get(timeout, TimeUnit.MILLISECONDS);
            
            URL furl = resPath.toUri().toURL();
            fhres = new FHBuilder(fhres).url(furl).build();
            
            File rootFile = fhres.getFilePath().toFile();
            AssertState.assertTrue(rootFile.exists(), "Cannot find IPFS content at: " + rootFile);
            
            if (rootFile.isDirectory()) {
            	fhres = createFHandleTree(fhres);
            } else {
            	fhres = createFromFileHeader(null, fhres);
            }
            
            fhres = new FHBuilder(fhres)
                    .available(true)
                    .build();
            
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
            fhres = new FHBuilder(fhres)
                    .elapsed(fhres.getElapsed() + elapsed)
                    .attempt(fhres.getAttempt() + 1)
                    .build();
            
            ipfsCache.put(fhres);
        }
        
        LOG.info("IPFS found: {}", fhres.toString(true));
        
        return fhres;
    }

    public List<FHandle> findContentAsync(Address owner, long timeout) {
        
    	WorkerFactory<FHandle> factory = new WorkerFactory<FHandle>() {

			@Override
			public Class<FHandle> getType() {
				return FHandle.class;
			}

			@Override
			public Callable<FHandle> newWorker(FHandle fh) {
				return new AsyncGetCallable(fh, timeout);
			}
		};
		
        List<FHandle> fhandles = findContentAsync(owner, factory, timeout);
        
        return fhandles;
    }

	public byte[] createFileData(FHandle fhandle) {
		return dataHandler.createFileData(fhandle);
	}
	
	@Override
    public FHandle getHandleFromTx(Address owner, UTXO utxo) {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(utxo, "Null utxo");

        Wallet wallet = cntmgr.getBlockchain().getWallet();
        Tx tx = wallet.getTransaction(utxo.getTxId());
        if (!isOurs(tx)) return null;
        
        FHandle fhandle = null;

        List<TxOutput> outs = tx.outputs();
        TxOutput out0 = outs.get(outs.size() - 2);
        TxOutput out1 = outs.get(outs.size() - 1);

        // Expect OP_FILE_DATA
        byte[] txdata = out1.getData();
        Multihash cid = dataHandler.extractFileData(txdata);
        Address outAddr = wallet.findAddress(out0.getAddress());
        if (cid != null && outAddr != null) {

            LOG.debug("File Tx: {} => {}", tx.txId(), cid);
            
            // Not owned by the given address
            if (!owner.equals(outAddr)) return null;
            
            fhandle = new FHBuilder(owner, tx.txId(), cid)
                    .owner(owner)
                    .build();
        }

        // The FHandle is not fully initialized
        // There has been no blocking IPFS access
        
        return fhandle;
    }
    
    public FHandle createFHandleTree(FHandle fhandle) throws IOException {
        
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
    
    private String logPrefix(String action, int attempt) {
    	ContentManagerConfig config = cntmgr.getConfig();
        int ipfsAttempts = config.getIpfsAttempts();
        String trdName = Thread.currentThread().getName();
        return String.format("IPFS %s [%s] [%d/%d]", action, trdName, attempt, ipfsAttempts);
    }
    
    class AsyncGetCallable implements Callable<FHandle> {
        
        final long timeout;
        final FHandle fhandle;
        
        AsyncGetCallable(FHandle fhandle, long timeout) {
            AssertArgument.assertNotNull(fhandle, "Null fhandle");
            this.timeout = timeout;
            this.fhandle = fhandle;
        }

        @Override
        public FHandle call() throws Exception {
            
        	IPFSCache ipfsCache = cntmgr.getIPFSCache();
        	
            FHandle fhaux = fhandle;
            Multihash cid = fhandle.getCid();
            
            try {
                
                fhaux = getIpfsContent(fhaux, timeout);
                
            } catch (Exception ex) {
                
                fhaux = processException(cid, ex);
                
            } finally {
                
                ipfsCache.put(fhaux);
            }

            return fhaux;
        }
        
        private FHandle processException(Multihash cid, Exception ex) throws InterruptedException {
            
            IPFSCache ipfsCache = cntmgr.getIPFSCache();
            ContentManagerConfig config = cntmgr.getConfig();
            
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
                
                LOG.warn("{}: {}", logPrefix("not found", attempt),  fhres);
            }
            
            else {
                
                fhres = new FHBuilder(fhres)
                        .expired(true)
                        .build();
                
                LOG.error(logPrefix("error", attempt) + ": " + fhres, ex);
            }
            
            return fhres;
        }
    }
}