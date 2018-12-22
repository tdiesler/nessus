package io.nessus.ipfs.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ipfs.multihash.Multihash;
import io.nessus.Tx;
import io.nessus.TxOutput;
import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.AHandle;
import io.nessus.ipfs.AbstractHandle;
import io.nessus.ipfs.FHandle;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;

abstract class AbstractHandleManager<T extends AbstractHandle> {
	
    final Logger LOG = LoggerFactory.getLogger(getClass());

	protected final DefaultContentManager cntmgr;
	protected final TxDataHandler dataHandler;
	protected final Executor executor;
    
	private static ExecutorService executorService;

	interface WorkerFactory<T extends AbstractHandle> {
		
		Class<T> getType();
		
		Callable<T> newWorker(T handle);
	}
	
	AbstractHandleManager(DefaultContentManager cntmgr) {
		this.cntmgr = cntmgr;
		
		if (executorService == null) {
            int ipfsThreads = cntmgr.getConfig().getIpfsThreads();
            executorService = Executors.newFixedThreadPool(ipfsThreads, new ThreadFactory() {
                AtomicInteger count = new AtomicInteger();
                public Thread newThread(Runnable run) {
                    return new Thread(run, "ipfs-pool-" + count.incrementAndGet());
                }
            });
		}

		FHeaderValues fhvals = cntmgr.getFHeaderValues();
        dataHandler = new TxDataHandler(fhvals);
        executor = new Executor();
	}

	List<T> findContentAsync(Address owner, WorkerFactory<T> factory, long timeout) {
		
		Class<T> type = factory.getType();
        
		// Get all unspent handles for a given type
		
        List<T> handles = listUnspentHandles(owner, type);
        
        // Process the workload asynchronously
        
        Future<Integer> future = executor.process(owner, handles, factory, timeout);
        
        int missing = -1;
		try {
			missing = future.get(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException ex) {
			LOG.error("{} error", getLogPrefix(type), ex);
		} catch (TimeoutException ex) {
			// ignore
		}

		if (missing > 0)
			LOG.error("{} still missing: {}", getLogPrefix(type), missing);
		
        // Get the current handles for the wanted cids
        
        List<Multihash> cids = getCids(handles);
        List<T> result = getCurrentHandles(cids, type);
        
		return result;
	}

	public T getUnspentHandle(Address owner, Multihash cid, Class<T> type) {
		AssertArgument.assertNotNull(owner, "Null owner");
		AssertArgument.assertNotNull(cid, "Null cid");
		
		T handle = listUnspentHandles(owner, type).stream()
			.filter(fh -> cid.equals(fh.getCid()))
			.findFirst().orElse(null);
		
		return handle;
	}
	
	public List<T> listUnspentHandles(Address owner, Class<T> type) {
		AssertArgument.assertNotNull(owner, "Null owner");
		
		// The list of handles that are recorded and unspent
        List<T> unspentFHandles = new ArrayList<>();
        
        IPFSCache ipfsCache = cntmgr.getIPFSCache();
    	Wallet wallet = cntmgr.getBlockchain().getWallet();
        
        synchronized (ipfsCache) {
        
            List<UTXO> locked = listLockedAndUnlockedUnspent(owner, true, false);
            List<UTXO> unspent = listLockedAndUnlockedUnspent(owner, true, true);
            
            for (UTXO utxo : unspent) {
                
                String txId = utxo.getTxId();
                Tx tx = wallet.getTransaction(txId);
                
                T txhdl = getHandleFromTx(owner, utxo);
                if (txhdl != null) {
                	
                	Multihash cid = txhdl.getCid();
                	T fhaux = ipfsCache.get(cid, type);
                	if (fhaux == null) {
                		ipfsCache.put(txhdl);
                		fhaux = txhdl;
                	}
                	
                    unspentFHandles.add(fhaux);
                    
                    // The lock state of a registration may get lost due to wallet 
                    // restart. Here we recreate that lock state if the given
                    // address owns the registration
                    
                    if (!locked.contains(utxo) && owner.getPrivKey() != null) {
                        
                        int vout = tx.outputs().size() - 2;
                        TxOutput dataOut = tx.outputs().get(vout);
                        AssertState.assertEquals(owner.getAddress(), dataOut.getAddress());
                        
                        wallet.lockUnspent(utxo, false);
                    }
                }
            }
            
            List<Multihash> unspentIds = unspentFHandles.stream()
            		.map(fh -> fh.getCid())
            		.collect(Collectors.toList());
            
            // Cleanup the cache by removing entries that are no longer unspent
            for (Multihash cid : new HashSet<>(ipfsCache.keySet(FHandle.class))) {
                if (!unspentIds.contains(cid)) {
                    ipfsCache.remove(cid, AbstractHandle.class);
                }
            }
        }
        
		return unspentFHandles;
	}

    abstract T getHandleFromTx(Address owner, UTXO utxo);

    boolean isOurs(Tx tx) {

        // Expect two outputs
        List<TxOutput> outs = tx.outputs();
        if (outs.size() < 2)
            return false;

        TxOutput out0 = outs.get(outs.size() - 2);
        TxOutput out1 = outs.get(outs.size() - 1);

        // Expect an address
        Wallet wallet = cntmgr.getBlockchain().getWallet();
        Address addr = wallet.findAddress(out0.getAddress());
        if (addr == null)
            return false;

        // Expect data on the second output
        if (out1.getData() == null)
            return false;

        // Expect data to be our's
        byte[] txdata = out1.getData();
        return dataHandler.isOurs(txdata);
    }

	List<UTXO> listLockedAndUnlockedUnspent(Address addr, boolean locked, boolean unlocked) {
        
    	Wallet wallet = cntmgr.getBlockchain().getWallet();
    	
        List<UTXO> result = new ArrayList<>();
        
        if (unlocked) {
            result.addAll(wallet.listUnspent(Arrays.asList(addr)));
        }
        
        if (locked) {
            result.addAll(wallet.listLockUnspent(Arrays.asList(addr)));
        }
        
        return result;
    }

    Address assertAddress(String rawAddr) {
    	Wallet wallet = cntmgr.getBlockchain().getWallet();
        Address addrs = wallet.findAddress(rawAddr);
        AssertState.assertNotNull(addrs, "Address not known to this wallet: " + rawAddr);
        return addrs;
    }

	private List<Multihash> getCids(List<T> fhandles) {
		
		List<Multihash> result = fhandles.stream()
	        .map(fh -> fh.getCid())
	        .collect(Collectors.toList());
		
		return result;
	}

    private List<T> getMissingHandles(List<T> fhandles, Class<T> type) {
    	
    	List<Multihash> cids = getCids(fhandles);
    	
        List<T> result = getCurrentHandles(cids, type).stream()
                .filter(fh -> cids.contains(fh.getCid()))
                .filter(fh -> fh.isMissing())
                .collect(Collectors.toList());
        
        return result;
    }

    private List<T> getCurrentHandles(List<Multihash> cids, Class<T> type) {
    	
    	IPFSCache ipfsCache = cntmgr.getIPFSCache();
    	
        List<T> result = ipfsCache.getAll(type).stream()
                .filter(fh -> cids.contains(fh.getCid()))
                .collect(Collectors.toList());
        
        return result;
    }

	private String getLogPrefix(Class<T> type) {
		boolean isAddr = type == AHandle.class;
		return isAddr ? "IPFS Addr" : "IPFS";
	}

    class Executor {
    	
    	final Set<Multihash> scheduled = new HashSet<>();

    	Future<Integer> process(Address owner, List<T> handles, WorkerFactory<T> factory, long timeout) {
    		
    		Class<T> type = factory.getType();
    		String prefix = getLogPrefix(type);
    		
            // Filter out available and expired handles 
            
            List<T> missing = getMissingHandles(handles, type);
            
            // Filter out handles that are already scheduled 
            
            List<T> unscheduled;
    		synchronized (scheduled) {
    			unscheduled = getUnscheduledHandles(missing);
    			scheduled.addAll(getCids(unscheduled));
    		}
        		
            // Log some scheduling stats
    		
            if (missing.size() > 0)
            	LOG.info("{} finding: [utxo={}, missing={}, schedule={}]", prefix, handles.size(), missing.size(), unscheduled.size());
            
            // For each unscheduled handle we create a task that keeps trying to get the 
            // content until it is no longer missing (i.e. either available or expired)
            
            long shortNap = 500L;
            
			for (T fh : unscheduled) {
				
				Multihash cid = fh.getCid();
				LOG.info("{} submit: {}", prefix, fh);
				
				executorService.submit(new Callable<Boolean>() {

					@Override
					public Boolean call() throws Exception {
						
						try {
							
							Future<T> submit = null;
							
							T aux = getUnspentHandle(owner, cid, type);
							
							while (aux != null && aux.isMissing()) {
								
								if (submit == null || submit.isDone()) {
		        					Callable<T> worker = factory.newWorker(aux);
		        					submit = executorService.submit(worker);
								}
								
	            				try {
	        						Thread.sleep(shortNap);
	        					} catch (InterruptedException e) {
	        						break;
	        					}
	            				
	            				// The UTXO may have been spent in the meantime
	            				
								aux = getUnspentHandle(owner, cid, type);
							}
							
						} finally {
							synchronized (scheduled) {
								scheduled.remove(cid);
							}
						}
						
						return fh.isAvailable();
					}
				});
			}

			Future<Integer> future = executorService.submit(new Callable<Integer>() {

				Map<Multihash, Integer> attempts = new HashMap<>();
				
				@Override
				public Integer call() throws Exception {
					
					// Record the curremt number of attempts for all missing handles
					missing.forEach(fh -> attempts.put(fh.getCid(), fh.getAttempt()));
										
					while(!completed()) {
						try {
							Thread.sleep(shortNap);
						} catch (InterruptedException e) {
							break;
						}
					}
					
					List<T> todos = getMissingHandles(missing, type);
					return todos.size();
				}
				
				boolean completed() {
					
					List<T> todos = getMissingHandles(missing, type);
					
					if (!todos.isEmpty()) {
						
						// Filter from todos when the next attempt has completed
						
						todos = todos.stream()
							.filter(fh -> {
						    	IPFSCache ipfsCache = cntmgr.getIPFSCache();
								T fhaux = ipfsCache.get(fh.getCid(), type);
								int lastAtt = attempts.get(fh.getCid());
								int currAtt = fhaux.getAttempt();
								return lastAtt == currAtt; 
							})
							.collect(Collectors.toList());
					}
					
					return todos.isEmpty();
				}
			});

			return future;
		}

        private List<T> getUnscheduledHandles(List<T> fhandles) {
        	
        	List<T> result = fhandles.stream()
                .filter(fh -> !scheduled.contains(fh.getCid()))
    	        .collect(Collectors.toList());
        	
        	return result;
        }
    }
}