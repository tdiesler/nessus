package io.nessus.ipfs;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.ipfs.multihash.Multihash;
import io.nessus.Wallet.Address;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;

public class AbstractHandle {
    
    protected final Address owner;
    protected final Multihash cid;
    protected final String txId;
    protected final boolean available;
    protected final boolean expired;
    protected final int attempt;
    protected final Long elapsed;
    
    protected final AtomicBoolean scheduled;
    
    protected AbstractHandle(Address owner, Multihash cid, String txId, boolean available, boolean expired, AtomicBoolean scheduled, int attempt, Long elapsed) {
        AssertArgument.assertNotNull(owner, "Null owner");
        
        this.owner = owner;
        this.cid = cid;
        this.txId = txId;
        this.available = available;
        this.expired = expired;
        this.scheduled = scheduled;
        this.attempt = attempt;
        this.elapsed = elapsed;
        
        List<String> labels = owner.getLabels();
        AssertState.assertTrue(labels.size() < 2, "Multiple labels: " + labels);
    }

    public Multihash getCid() {
        return cid;
    }

    public Address getOwner() {
        return owner;
    }
    
	public String getLabel() {
		return owner.getLabels().get(0);
	}

    public String getTxId() {
        return txId;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean setScheduled(boolean flag) {
        return scheduled.compareAndSet(!flag, flag);
    }

    public boolean isScheduled() {
        return scheduled.get();
    }

    public int getAttempt() {
        return attempt;
    }

    public Long getElapsed() {
        return elapsed;
    }

    public boolean isMissing() {
        return !available && !expired;
    } 
    
    @Override
    public String toString() {
    	String cidstr = cid != null ? cid.toBase58() : null;
        return String.format("[cid=%s, owner=%s, avl=%d, exp=%d, try=%d, time=%s]", 
        		cidstr, owner.getAddress(), available ? 1 : 0, expired ? 1 : 0, attempt, elapsed);
    }
}
