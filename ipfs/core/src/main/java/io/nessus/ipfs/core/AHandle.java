package io.nessus.ipfs.core;

import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicBoolean;

import io.ipfs.multihash.Multihash;
import io.nessus.Wallet.Address;
import io.nessus.cipher.utils.AESUtils;
import io.nessus.ipfs.AbstractHandle;

public class AHandle extends AbstractHandle {
	
	final PublicKey pubKey;
	
	private AHandle(Address addr, PublicKey pubKey, Multihash cid, String txId, boolean expired, AtomicBoolean scheduled, int attempt, Long elapsed) {
    	super(addr, cid, txId, pubKey != null, expired, scheduled, attempt, elapsed);
		this.pubKey = pubKey;
	}
	
	public PublicKey getPubKey() {
		return pubKey;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + owner.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof AHandle)) return false;
		AHandle other = (AHandle) obj;
		return owner.equals(other.owner);
	}

	@Override
	public String toString() {
        String addr = owner.getAddress();
        String cidstr = cid != null ? cid.toBase58() : null;
        String keystr = pubKey != null ? AESUtils.encodeKey(pubKey).substring(0, 8) : null;
		return String.format("[addr=%s, cid=%s, key=%s, avl=%d, exp=%d, try=%d, time=%s]", 
				addr, cidstr, keystr, available ? 1 : 0, expired ? 1 : 0, attempt, elapsed);
	}
	

    public static class AHBuilder {
        
        private Address owner;
        private PublicKey pubKey;
        private Multihash cid;
        private String txId;
        private boolean expired;
        private int attempt;
        private Long elapsed;
        
        private AtomicBoolean scheduled = new AtomicBoolean();
        
        public AHBuilder(Address owner, PublicKey pubKey) {
            this.owner = owner;
            this.pubKey = pubKey;
        }

        public AHBuilder(Address owner, String txId, Multihash cid) {
            this.owner = owner;
            this.txId = txId;
            this.cid = cid;
        }

        public AHBuilder(AHandle ahandle) {
            this.owner = ahandle.owner;
            this.pubKey = ahandle.pubKey;
            this.cid = ahandle.cid;
            this.txId = ahandle.txId;
            this.expired = ahandle.expired;
            this.scheduled = ahandle.scheduled;
            this.attempt = ahandle.attempt;
            this.elapsed = ahandle.elapsed;
        }

        public AHBuilder owner(Address owner) {
            this.owner = owner;
            return this;
        }
        
        public AHBuilder pubKey(PublicKey pubKey) {
            this.pubKey = pubKey;
            return this;
        }
        
        public AHBuilder cid(Multihash cid) {
            this.cid = cid;
            return this;
        }
        
        public AHBuilder txId(String txId) {
            this.txId = txId;
            return this;
        }
        
        public AHBuilder expired(boolean expired) {
            this.expired = expired;
            return this;
        }
        
        public AHBuilder attempt(int attempt) {
            this.attempt = attempt;
            return this;
        }
        
        public AHBuilder elapsed(long millis) {
            this.elapsed = elapsed != null ? elapsed + millis : millis;
            return this;
        }
        
        public AHandle build() {
        	AHandle ahandle = new AHandle(owner, pubKey, cid, txId, expired, scheduled, attempt, elapsed);
            return ahandle;
        }
    }
}