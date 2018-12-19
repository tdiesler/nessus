package io.nessus.ipfs;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.ipfs.multihash.Multihash;
import io.nessus.Wallet.Address;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;

public class AbstractHandle {
    
    protected final Address owner;
    protected final CidPath cid;
    protected final String txId;
    protected final boolean available;
    protected final boolean expired;
    protected final int attempt;
    protected final long elapsed;
    
    protected final AtomicBoolean scheduled;
    
    protected AbstractHandle(Address owner, CidPath cid, String txId, boolean available, boolean expired, AtomicBoolean scheduled, int attempt, long elapsed) {
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
    	if (cid == null) return null;
    	if (cid.getPath() != null) return null;
        return cid.getCid();
    }

    public CidPath getCidPath() {
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

    public long getElapsed() {
        return elapsed;
    }

    public boolean isMissing() {
        return !available && !expired;
    } 
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + owner.hashCode();
		result = prime * result + ((cid == null) ? 0 : cid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (cid == null) return false;
		if (getClass() != obj.getClass()) return false;
		AbstractHandle other = (AbstractHandle) obj;
		return owner.equals(other.owner) && cid.equals(other.cid);
	}

	protected static abstract class AbstractBuilder<B extends AbstractBuilder<?, ?>, T extends AbstractHandle> {
        
        protected Address owner;
        protected CidPath cid;
        protected String txId;
        protected boolean expired;
        protected int attempt;
        protected long elapsed;
        
        protected AtomicBoolean scheduled = new AtomicBoolean();
        
        protected AbstractBuilder(Address owner) {
            AssertArgument.assertNotNull(owner, "Null owner");
            this.owner = owner;
        }

        protected AbstractBuilder(Address owner, String txId, Multihash cid) {
            AssertArgument.assertNotNull(owner, "Null owner");
            AssertArgument.assertNotNull(txId, "Null txId");
            AssertArgument.assertNotNull(cid, "Null cid");
            this.cid = new CidPath(cid);
            this.owner = owner;
            this.txId = txId;
        }

        protected AbstractBuilder(T handle) {
            AssertArgument.assertNotNull(handle, "Null handle");
            this.owner = handle.owner;
            this.cid = handle.cid;
            this.txId = handle.txId;
            this.expired = handle.expired;
            this.scheduled = handle.scheduled;
            this.attempt = handle.attempt;
            this.elapsed = handle.elapsed;
        }

        @SuppressWarnings("unchecked")
		public B owner(Address owner) {
            this.owner = owner;
            return (B) this;
        }
        
        @SuppressWarnings("unchecked")
        public B cid(Multihash cid) {
            this.cid = cid != null ? new CidPath(cid) : null;
            return (B) this;
        }
        
        @SuppressWarnings("unchecked")
        public B txId(String txId) {
            this.txId = txId;
            return (B) this;
        }
        
        @SuppressWarnings("unchecked")
        public B expired(boolean expired) {
            this.expired = expired;
            return (B) this;
        }
        
        @SuppressWarnings("unchecked")
        public B attempt(int attempt) {
            this.attempt = attempt;
            return (B) this;
        }
        
        @SuppressWarnings("unchecked")
        public B elapsed(long elapsed) {
            this.elapsed = elapsed;
            return (B) this;
        }
        
        public abstract T build();
    }
}
