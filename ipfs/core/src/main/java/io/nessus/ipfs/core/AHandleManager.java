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
import java.util.List;
import java.util.Properties;
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
import io.nessus.cipher.utils.RSAUtils;
import io.nessus.ipfs.AHandle;
import io.nessus.ipfs.AHandle.AHBuilder;
import io.nessus.ipfs.ContentManagerConfig;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.IPFSException;
import io.nessus.ipfs.IPFSNotFoundException;
import io.nessus.ipfs.IPFSTimeoutException;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;

public class AHandleManager extends AbstractHandleManager<AHandle> {

	private static final String KEY_LABEL = "Label";
	private static final String KEY_ADDRESS = "Address";
	private static final String KEY_PUBKEY = "PublicKey";

	AHandleManager(DefaultContentManager cntmgr) {
		super(cntmgr);
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

	public AHandle getIpfsContent(AHandle ahandle, long timeout) throws IOException, GeneralSecurityException {
        AssertArgument.assertNotNull(ahandle, "Null ahandle");
        AssertArgument.assertNotNull(ahandle.getOwner(), "Null owner");
        AssertArgument.assertNotNull(ahandle.getCid(), "Null cid");

        Address owner = ahandle.getOwner();
        Multihash cid = ahandle.getCid();
        
        IPFSCache ipfsCache = cntmgr.getIPFSCache();
        AHandle ahres = ipfsCache.get(cid, AHandle.class);
        if (ahres.isAvailable()) return ahres;
        
        // Fetch the content from IPFS
        
        int attempt = ahres.getAttempt() + 1;
		ahres = new AHBuilder(ahres)
                .attempt(attempt)
                .build();
        
        LOG.info("{}: {}", logPrefix("attempt", attempt),  ahres);
        
    	long before = System.currentTimeMillis();
		
        try {
            
    		IPFSClient ipfsClient = cntmgr.getIPFSClient();
    		FHeaderValues fhvals = cntmgr.getFHeaderValues();

    		Properties props = new Properties();
    		Future<InputStream> future = ipfsClient.cat(ahres.getCid());
    		InputStream input = future.get(timeout, TimeUnit.MILLISECONDS);
    		props.load(input);

    		String version = props.getProperty(fhvals.PREFIX + "-Version");
    		String rawAddr = props.getProperty(KEY_ADDRESS);
    		String encKey = props.getProperty(KEY_PUBKEY);

    		AssertState.assertEquals(fhvals.VERSION, version);
            AssertState.assertEquals(owner.getAddress(), rawAddr, "Unexpected owner: " + rawAddr);
            
    		PublicKey pubKey = RSAUtils.decodePublicKey(encKey);
    		ahres = new AHBuilder(ahres)
    				.pubKey(pubKey)
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
            ahres = new AHBuilder(ahres)
    				.elapsed(ahres.getElapsed() + elapsed)
    				.build();
            
            ipfsCache.put(ahres);
        }
        
        LOG.info("IPFS Addr found: {}", ahres);

		return ahres;
	}

    public AHandle findContentAsync(Address owner, long timeout) {
        
    	WorkerFactory<AHandle> factory = new WorkerFactory<AHandle>() {

			@Override
			Class<AHandle> getType() {
				return AHandle.class;
			}

			@Override
			Callable<AHandle> newWorker(AHandle fh) {
				return new AsyncGetCallable(fh, timeout);
			}
		};
		
        List<AHandle> ahandles = findContentAsync(owner, factory, timeout);
        AHandle ahres = ahandles.stream().findFirst().orElse(null);
        
        return ahres;
    }

	public byte[] createAddrData(Multihash cid) {
		return dataHandler.createAddrData(cid);
	}
	
    @Override
    public AHandle getHandleFromTx(Address owner, UTXO utxo) {
        AssertArgument.assertNotNull(owner, "Null owner");
        AssertArgument.assertNotNull(utxo, "Null utxo");

        Wallet wallet = cntmgr.getBlockchain().getWallet();
        Tx tx = wallet.getTransaction(utxo.getTxId());
        if (!isOurs(tx)) return null;
        
        AHandle ahandle = null;

        List<TxOutput> outs = tx.outputs();
        TxOutput out0 = outs.get(outs.size() - 2);
        TxOutput out1 = outs.get(outs.size() - 1);

        // Expect OP_ADDR_DATA
        byte[] txdata = out1.getData();
        Multihash cid = dataHandler.extractAddrData(txdata);
        Address outAddr = wallet.findAddress(out0.getAddress());
        if (cid != null && outAddr != null) {

            LOG.debug("Addr Tx: {} => {}", tx.txId(), cid);
            
            // Not owned by the given address
            if (!owner.equals(outAddr)) return null;
            
            ahandle = new AHBuilder(owner, tx.txId(), cid).build();
        }

        // The AHandle is not fully initialized
        // There has been no blocking IPFS access
        
        return ahandle;
    }

    private String logPrefix(String action, int attempt) {
    	ContentManagerConfig config = cntmgr.getConfig();
        int ipfsAttempts = config.getIpfsAttempts();
        String trdName = Thread.currentThread().getName();
        return String.format("IPFS Addr %s [%s] [%d/%d]", action, trdName, attempt, ipfsAttempts);
    }
    
    class AsyncGetCallable implements Callable<AHandle> {
        
        final long timeout;
        final AHandle ahandle;
        
        AsyncGetCallable(AHandle ahandle, long timeout) {
            AssertArgument.assertNotNull(ahandle, "Null ahandle");
            this.timeout = timeout;
            this.ahandle = ahandle;
        }

        @Override
        public AHandle call() throws Exception {
            
            IPFSCache ipfsCache = cntmgr.getIPFSCache();
            
        	AHandle ahaux = ahandle;
        	Multihash cid = ahandle.getCid();
        	
            try {
                
                ahaux = getIpfsContent(ahandle, timeout);
                
            } catch (Exception ex) {
                
                ahaux = processException(cid, ex);
                
            } finally {
                
                ipfsCache.put(ahaux);
            }

            return ahaux;
        }
        
        private AHandle processException(Multihash cid, Exception ex) throws InterruptedException {
            
            IPFSCache ipfsCache = cntmgr.getIPFSCache();
            ContentManagerConfig config = cntmgr.getConfig();
            
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
                
                LOG.warn("{}: {}", logPrefix("not found", attempt),  ahres);
            }
            
            else {
                
                ahres = new AHBuilder(ahres)
                        .expired(true)
                        .build();
                
                LOG.error(logPrefix("error", attempt) + ": " + ahres, ex);
            }
            
            return ahres;
        }
    }
}