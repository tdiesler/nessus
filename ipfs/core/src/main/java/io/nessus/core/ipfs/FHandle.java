package io.nessus.core.ipfs;

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

import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import io.nessus.Wallet.Address;

public class FHandle {
    
    final Address owner;
    final String cid;
    final Path path;
    final URL furl;
    final String txId;
    final String secToken;
    final boolean available;
    final boolean expired;
    final int attempt;
    final Long elapsed;
    
    final AtomicBoolean scheduled;
    
    private FHandle(String cid, Path path, Address owner, URL furl, String secToken, String txId, boolean available, boolean expired, AtomicBoolean scheduled, int attempt, Long elapsed) {
        this.owner = owner;
        this.path = path;
        this.furl = furl;
        this.cid = cid;
        this.txId = txId;
        this.secToken = secToken;
        this.available = available;
        this.expired = expired;
        this.scheduled = scheduled;
        this.attempt = attempt;
        this.elapsed = elapsed;
    }

    public String getCid() {
        return cid;
    }

    public URL getURL() {
        return furl;
    }

    public Path getPath() {
        return path;
    }
    
    public Address getOwner() {
        return owner;
    }
    
    public String getTxId() {
        return txId;
    }

    public String getSecretToken() {
        return secToken;
    }

    public boolean isEncrypted() {
        return secToken != null;
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
    
    public String toString() {
        return String.format("[cid=%s, owner=%s, path=%s, avl=%d, exp=%d, try=%d, time=%s]", 
                cid, owner.getAddress(), path, available ? 1 : 0, expired ? 1 : 0, attempt, elapsed);
    }
    
    public static class FHBuilder {
        
        private Address owner;
        private String cid;
        private Path path;
        private URL furl;
        private String txId;
        private String secToken;
        private boolean available;
        private boolean expired;
        private int attempt;
        private Long elapsed;
        
        private AtomicBoolean scheduled = new AtomicBoolean();
        
        public FHBuilder(FHandle fhandle) {
            this.owner = fhandle.owner;
            this.cid = fhandle.cid;
            this.path = fhandle.path;
            this.furl = fhandle.furl;
            this.txId = fhandle.txId;
            this.secToken = fhandle.secToken;
            this.available = fhandle.available;
            this.expired = fhandle.expired;
            this.scheduled = fhandle.scheduled;
            this.attempt = fhandle.attempt;
            this.elapsed = fhandle.elapsed;
        }

        public FHBuilder(URL furl) {
            this.furl = furl;
        }

        public FHBuilder(String cid) {
            this.cid = cid;
        }

        public FHBuilder cid (String cid) {
            this.cid = cid;
            return this;
        }
        
        public FHBuilder path (Path path) {
            this.path = path;
            return this;
        }
        
        public FHBuilder owner(Address owner) {
            this.owner = owner;
            return this;
        }
        
        public FHBuilder url (URL furl) {
            this.furl = furl;
            return this;
        }
        
        public FHBuilder secretToken(String secToken) {
            this.secToken = secToken;
            return this;
        }
        
        public FHBuilder txId(String txId) {
            this.txId = txId;
            return this;
        }
        
        public FHBuilder available(boolean available) {
            this.available = available;
            return this;
        }
        
        public FHBuilder expired(boolean expired) {
            this.expired = expired;
            return this;
        }
        
        public FHBuilder attempt(int attempt) {
            this.attempt = attempt;
            return this;
        }
        
        public FHBuilder elapsed(long millis) {
            this.elapsed = elapsed != null ? elapsed + millis : millis;
            return this;
        }
        
        public FHandle build() {
            return new FHandle(cid, path, owner, furl, secToken, txId, available, expired, scheduled, attempt, elapsed);
        }
    }
}
