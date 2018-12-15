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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.ipfs.multihash.Multihash;
import io.nessus.Wallet.Address;
import io.nessus.cipher.utils.RSAUtils;
import io.nessus.ipfs.Config;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.IPFSException;
import io.nessus.ipfs.IPFSNotFoundException;
import io.nessus.ipfs.IPFSTimeoutException;
import io.nessus.ipfs.core.AHandle.AHBuilder;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;

public class AHandleManager extends AbstractHandleManager {

	private static final String KEY_LABEL = "Label";
	private static final String KEY_ADDRESS = "Address";
	private static final String KEY_PUBKEY = "PublicKey";

	AHandleManager(DefaultContentManager cntmgr) {
		super(cntmgr);
	}

	public AHandle getIpfsContent(AHandle ahandle, long timeout) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(ahandle, "Null ahandle");
        AssertArgument.assertNotNull(ahandle.getOwner(), "Null owner");
        AssertArgument.assertNotNull(ahandle.getCid(), "Null cid");

		IPFSClient ipfsClient = cntmgr.getIPFSClient();
		FHeaderValues fhvals = cntmgr.getFHeaderValues();

    	AHandle ahres = new AHBuilder(ahandle).build();
		
    	long before = System.currentTimeMillis();
		
        try {
            
    		Properties props = new Properties();
    		Future<InputStream> future = ipfsClient.cat(ahres.getCid());
    		InputStream input = future.get(timeout, TimeUnit.MILLISECONDS);
    		props.load(input);

    		String version = props.getProperty(fhvals.PREFIX + "-Version");
    		String rawAddr = props.getProperty(KEY_ADDRESS);
    		String encKey = props.getProperty(KEY_PUBKEY);

    		AssertState.assertEquals(fhvals.VERSION, version);

    		Address owner = assertAddress(rawAddr);
            AssertState.assertEquals(ahres.getOwner(), owner, "Unexpected owner: " + owner);
            
    		PublicKey pubKey = RSAUtils.decodePublicKey(encKey);
    		ahres = new AHBuilder(ahres)
    				.pubKey(pubKey)
    				.build();
            
            LOG.info("IPFS Addr found: {}", ahres);

        } catch (InterruptedException | ExecutionException ex) {
            
            Throwable cause = ex.getCause();
            if (cause instanceof IPFSException) 
                throw (IPFSException)cause;
            else 
                throw new IPFSException(ex);
            
        } catch (TimeoutException ex) {
            
            throw new IPFSTimeoutException(ex);
            
        } finally {
            
            long elapsed = ahres.getElapsed() + System.currentTimeMillis() - before;
            ahres = new AHBuilder(ahres)
    				.elapsed(elapsed)
    				.build();
            
            IPFSCache ipfsCache = cntmgr.getIPFSCache();
            ipfsCache.put(ahres, AHandle.class);
        }
        
		return ahres;
	}

	public AHandle addIpfsContent(AHandle ahandle, boolean dryRun) throws IOException {

		IPFSClient ipfsClient = cntmgr.getIPFSClient();
		FHeaderValues fhvals = cntmgr.getFHeaderValues();

		Address owner = ahandle.getOwner();
		String rawAddr = owner.getAddress();
		PublicKey pubKey = ahandle.getPubKey();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos))) {
			pw.println(fhvals.PREFIX + "-Version: " + fhvals.VERSION);
			pw.println(KEY_ADDRESS + ": " + rawAddr);
			pw.println(KEY_LABEL + ": " + ahandle.getLabel());
			pw.println(KEY_PUBKEY + ": " + RSAUtils.encodeKey(pubKey));
		}

		Multihash cid = ipfsClient.addSingle(baos.toByteArray(), dryRun);
		AHandle ahres = new AHBuilder(ahandle).cid(cid).build();

		return ahres;
	}

    public AHandle findIpfsContentAsync(AHandle ahandle, long timeout) {
        
    	IPFSCache ipfsCache = cntmgr.getIPFSCache();
    	Multihash cid = ahandle.getCid();
    	
    	AHandle ahres;
    	
        synchronized (ipfsCache) {
        	ahres = ipfsCache.get(cid, AHandle.class);
            if (ahres == null) {
            	ahres = new AHBuilder(ahandle).elapsed(0L).build();
                LOG.info("IPFS Addr submit: {}", ahres);
                ipfsCache.put(ahres, AHandle.class);
            }
        }
        
        if (ahres.isAvailable())
        	return ahres;
        
        Future<AHandle> future = executorService.submit(new Callable<AHandle>() {

            @Override
            public AHandle call() throws Exception {
                
            	AHandle ahaux = ipfsCache.get(cid, AHandle.class);
        		
                while(!ahaux.isAvailable()) {
                    if (ahaux.setScheduled(true)) {
                        AsyncGetCallable callable = new AsyncGetCallable(ahaux, timeout);
                        executorService.submit(callable);
                    }
                    Thread.sleep(500L);
                	ahaux = ipfsCache.get(cid, AHandle.class);
                }
                
                return ahaux;
            }
        });
        
        try {
        	ahres = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException ex) {
            throw new IllegalStateException(ex);
        } catch (TimeoutException ex) {
        	// ignore
        }
        
        return ahres;
    }

    class AsyncGetCallable implements Callable<AHandle> {
        
        final long timeout;
        final AHandle ahandle;
        
        AsyncGetCallable(AHandle ahandle, long timeout) {
            AssertArgument.assertNotNull(ahandle, "Null ahandle");
            AssertArgument.assertTrue(ahandle.isScheduled(), "Not scheduled");
            this.timeout = timeout;
            this.ahandle = ahandle;
        }

        @Override
        public AHandle call() throws Exception {
            
            int attempt = ahandle.getAttempt() + 1;
            AHandle ahaux = new AHBuilder(ahandle)
                    .attempt(attempt)
                    .build();
            
            IPFSCache ipfsCache = cntmgr.getIPFSCache();
            ipfsCache.put(ahaux, AHandle.class);
            
            LOG.info("{}: {}", logPrefix("attempt", attempt),  ahaux);
            
            try {
                
                ahaux = getIpfsContent(ahaux, timeout);
                
            } catch (Exception ex) {
                
                ahaux = processException(ahaux, ex);
                
            } finally {
                
                ahaux.setScheduled(false);
                ipfsCache.put(ahaux, AHandle.class);
            }

            return ahaux;
        }
        
        private AHandle processException(AHandle ahandle, Exception ex) throws InterruptedException {
            
            IPFSCache ipfsCache = cntmgr.getIPFSCache();
            Config config = cntmgr.getConfig();
            
            Multihash cid = ahandle.getCid();
            AHandle ahres = ipfsCache.get(cid, AHandle.class);
            int attempt = ahres.getAttempt();
            
            if (ex instanceof IPFSTimeoutException) {
                
                if (config.getIpfsAttempts() <= attempt) {
                    ahres = new AHBuilder(ahres)
                            .expired(true)
                            .build();
                }
                
                LOG.info("{}: {}", logPrefix("timeout", attempt),  ahres);
            }
            
            else if (ex instanceof IPFSNotFoundException) {
                
                ahres = new AHBuilder(ahres)
                        .expired(true)
                        .build();
                
                LOG.warn("{}: {}", logPrefix("no merkle", attempt),  ahres);
            }
            
            else {
                
                ahres = new AHBuilder(ahres)
                        .expired(true)
                        .build();
                
                LOG.error(logPrefix("error", attempt) + ": " + ahres, ex);
            }
            
            return ahres;
        }

        private String logPrefix(String action, int attempt) {
            Config config = cntmgr.getConfig();
            int ipfsAttempts = config.getIpfsAttempts();
            String trdName = Thread.currentThread().getName();
            return String.format("IPFS Addr %s [%s] [%d/%d]", action, trdName, attempt, ipfsAttempts);
        }
    }
}