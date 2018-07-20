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

import java.net.URL;
import java.nio.file.Path;

import io.nessus.Wallet.Address;

public class FHandle {
    
    final Address owner;
    final String cid;
    final Path path;
    final URL furl;
    final String txId;
    final String secToken;
    
    private FHandle(String cid, Path path, Address owner, URL furl, String secToken, String txId) {
        this.owner = owner;
        this.path = path;
        this.furl = furl;
        this.cid = cid;
        this.txId = txId;
        this.secToken = secToken;
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
    
    public String getTx() {
        return txId;
    }

    public String getSecretToken() {
        return secToken;
    }

    public boolean isEncrypted() {
        return secToken != null;
    }

    public String toString() {
        return String.format("[cid=%s, owner=%s, path=%s, url=%s, tx=%s]", cid, owner.getAddress(), path, furl, txId);
    }
    
    public static class FHBuilder {
        
        private Address owner;
        private String cid;
        private Path path;
        private URL furl;
        private String txId;
        private String secToken;
        
        public FHBuilder(FHandle fhandle) {
            this.owner = fhandle.owner;
            this.cid = fhandle.cid;
            this.path = fhandle.path;
            this.furl = fhandle.furl;
            this.txId = fhandle.txId;
            this.secToken = fhandle.secToken;
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
        
        public FHandle build() {
            return new FHandle(cid, path, owner, furl, secToken, txId);
        }
    }
}
