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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.List;

import io.nessus.Blockchain;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.impl.DefaultContentManager;
import io.nessus.utils.AssertState;

public interface ContentManager {

    /**
     * Get the Blockchain
     */
    Blockchain getBlockchain();
    
    /**
     * Get the IPFS client
     */
    IPFSClient getIPFSClient();
    
    /**
     * Register a public address.
     * 
     * @return An Eliptic Curve public key
     */
    PublicKey registerAddress(Address addr) throws GeneralSecurityException;

    /**
     * Find the registered key for a given address.
     * 
     * @return An Eliptic Curve public key
     */
    PublicKey findAddressRegistation(Address addr);
    
    /**
     * Unregister a public address.
     */
    PublicKey unregisterAddress(Address addr);
    
    /**
     * Add content to IPFS.
     */
    FHandle addIpfsContent(Address owner, Path dstPath, URL srcUrl) throws IOException, GeneralSecurityException;
    
    /**
     * Add content to IPFS.
     */
    FHandle addIpfsContent(Address owner, Path dstPath, InputStream input) throws IOException, GeneralSecurityException;
    
    /**
     * Add content to IPFS from local file system.
     */
    FHandle addIpfsContent(Address owner, Path srcPath) throws IOException, GeneralSecurityException;
    
    /**
     * Get content from IPFS. 
     */
    FHandle getIpfsContent(Address owner, String cid, Path path, Long timeout) throws IOException, GeneralSecurityException;
    
    /**
     * Find registered IPFS content for a given address.
     */
    List<FHandle> findIpfsContent(Address owner, Long timeout) throws IOException;
    
    /**
     * Send content to a target address via IPFS. 
     */
    FHandle sendIpfsContent(Address owner, String cid, Address target, Long timeout) throws IOException, GeneralSecurityException;

    /**
     * Unregister a IPFS content.
     */
    List<String> unregisterIpfsContent(Address owner, List<String> cids) throws IOException;
    
    /**
     * Show content of a plain file from local storage.  
     */
    InputStream getLocalContent(Address owner, Path path) throws IOException;
    
    /**
     * Find local content for a given address.
     */
    List<FHandle> findLocalContent(Address owner) throws IOException;
    
    /**
     * Find local content for a given address and path.
     */
    FHandle findLocalContent(Address owner, Path path) throws IOException;
    
    /**
     * Remove a plain file content from local storage.  
     */
    boolean removeLocalContent(Address owner, Path path) throws IOException;


    public class ContentManagerConfig {

        private final Blockchain blockchain;
        private final IPFSClient ipfsClient;
        private long ipfsTimeout = DefaultContentManager.DEFAULT_IPFS_TIMEOUT;
        private int ipfsAttempts = DefaultContentManager.DEFAULT_IPFS_ATTEMPTS;
        private int ipfsThreads = DefaultContentManager.DEFAULT_IPFS_THREADS;
        private Path rootPath = Paths.get(System.getProperty("user.home"), ".nessus");
        private boolean replaceExisting;
        
        private boolean mutable = true;
        
        public ContentManagerConfig(Blockchain blockchain, IPFSClient ipfsClient) {
            this.blockchain = blockchain;
            this.ipfsClient = ipfsClient;
        }

        public Blockchain getBlockchain() {
            return blockchain;
        }

        public IPFSClient getIpfsClient() {
            return ipfsClient;
        }

        public long getIpfsTimeout() {
            return ipfsTimeout;
        }

        public int getIpfsAttempts() {
            return ipfsAttempts;
        }

        public int getIpfsThreads() {
            return ipfsThreads;
        }

        public Path getRootPath() {
            return rootPath;
        }

        public boolean isReplaceExisting() {
            return replaceExisting;
        }

        public ContentManagerConfig ipfsTimeout(long ipfsTimeout) {
            assertMutable();
            this.ipfsTimeout = ipfsTimeout;
            return this;
        }

        public ContentManagerConfig ipfsAttempts(int ipfsAttempts) {
            assertMutable();
            this.ipfsAttempts = ipfsAttempts;
            return this;
        }

        public ContentManagerConfig ipfsThreads(int ipfsThreads) {
            assertMutable();
            this.ipfsThreads = ipfsThreads;
            return this;
        }

        public ContentManagerConfig rootPath(Path rootPath) {
            assertMutable();
            this.rootPath = rootPath;
            return this;
        }

        public ContentManagerConfig replaceExisting() {
            assertMutable();
            this.replaceExisting = true;
            return this;
        }

        public ContentManagerConfig makeImmutable() {
            mutable = false;
            return this;
        }
        
        private void assertMutable() {
            AssertState.assertTrue(mutable, "Immutable config");
        }
        
        public String toString() {
            return String.format("[rootPath=%s, timeout=%s, attempts=%s, threads=%s, replace=%b]", 
                    rootPath, ipfsTimeout, ipfsAttempts, ipfsThreads, replaceExisting);
        }
    }
}
