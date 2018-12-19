package io.nessus.ipfs.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
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
import io.nessus.ipfs.AbstractHandle;
import io.nessus.ipfs.ContentManagerConfig;
import io.nessus.ipfs.FHandle;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;

abstract class AbstractHandleManager<T extends AbstractHandle> {
	
    final Logger LOG = LoggerFactory.getLogger(getClass());

	protected final DefaultContentManager cntmgr;
	protected final ExecutorService executorService;
	protected final TxDataHandler dataHandler;
    
	AbstractHandleManager(DefaultContentManager cntmgr) {
		this.cntmgr = cntmgr;
		
		FHeaderValues fhvals = cntmgr.getFHeaderValues();
        dataHandler = new TxDataHandler(fhvals);

		ContentManagerConfig config = cntmgr.getConfig();
        int ipfsThreads = config.getIpfsThreads();
        executorService = Executors.newFixedThreadPool(ipfsThreads, new ThreadFactory() {
            AtomicInteger count = new AtomicInteger();
            public Thread newThread(Runnable run) {
                return new Thread(run, "ipfs-pool-" + count.incrementAndGet());
            }
        });
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
                		LOG.info("IPFS submit: {}", txhdl);
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

}