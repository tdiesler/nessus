package io.nessus.ipfs.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.Config;
import io.nessus.utils.AssertState;

class AbstractHandleManager {
	
    final Logger LOG = LoggerFactory.getLogger(getClass());

	protected final DefaultContentManager cntmgr;
	protected final ExecutorService executorService;
    
	AbstractHandleManager(DefaultContentManager cntmgr) {
		this.cntmgr = cntmgr;
		
		Config config = cntmgr.getConfig();
        int ipfsThreads = config.getIpfsThreads();
        executorService = Executors.newFixedThreadPool(ipfsThreads, new ThreadFactory() {
            AtomicInteger count = new AtomicInteger();
            public Thread newThread(Runnable run) {
                return new Thread(run, "ipfs-pool-" + count.incrementAndGet());
            }
        });
	}

    Address assertAddress(String rawAddr) {
    	Wallet wallet = cntmgr.getBlockchain().getWallet();
        Address addrs = wallet.findAddress(rawAddr);
        AssertState.assertNotNull(addrs, "Address not known to this wallet: " + rawAddr);
        return addrs;
    }

}