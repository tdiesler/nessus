package io.nessus.ipfs;

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

import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicBoolean;

import io.ipfs.multihash.Multihash;
import io.nessus.Wallet.Address;
import io.nessus.cipher.utils.AESUtils;
import io.nessus.utils.AssertArgument;

public class AHandle extends AbstractHandle {
	
	final PublicKey pubKey;
	
	private AHandle(Address addr, PublicKey pubKey, CidPath cid, String txId, boolean expired, AtomicBoolean scheduled, int attempt, long elapsed) {
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
        String keystr = pubKey != null ? AESUtils.encodeKey(pubKey).substring(0, 8) : null;
		return String.format("[addr=%s, cid=%s, key=%s, avl=%d, exp=%d, try=%d, time=%s]", 
				addr, cid, keystr, available ? 1 : 0, expired ? 1 : 0, attempt, elapsed);
	}

    public static class AHBuilder extends AbstractBuilder<AHBuilder, AbstractHandle> {
        
        private PublicKey pubKey;
        
        public AHBuilder(Address owner, PublicKey pubKey) {
        	super(owner);
            AssertArgument.assertNotNull(pubKey, "Null pubKey");
            this.pubKey = pubKey;
        }

        public AHBuilder(Address owner, String txId, Multihash cid) {
        	super(owner, txId, cid);
        }

        public AHBuilder(AHandle ahandle) {
        	super(ahandle);
            this.pubKey = ahandle.pubKey;
        }

        public AHBuilder pubKey(PublicKey pubKey) {
            this.pubKey = pubKey;
            return this;
        }

        @Override
        public AHandle build() {
        	AHandle ahandle = new AHandle(owner, pubKey, cid, txId, expired, scheduled, attempt, elapsed);
            return ahandle;
        }
    }
}