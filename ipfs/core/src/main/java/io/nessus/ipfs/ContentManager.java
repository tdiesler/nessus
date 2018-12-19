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
import java.security.GeneralSecurityException;
import java.util.List;

import io.ipfs.multihash.Multihash;
import io.nessus.Blockchain;
import io.nessus.Wallet.Address;

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
     * Create the public encyption key for a given address.
     */
    AHandle registerAddress(Address addr) throws GeneralSecurityException, IOException;

    /**
     * Find the public encyption key for a given address.
     */
    AHandle findAddressRegistation(Address addr, Long timeout);
    
    /**
     * Unregister the public encyption key for a given address.
     */
    AHandle unregisterAddress(Address addr);
    
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
    FHandle getIpfsContent(Address owner, Multihash cid, Path path, Long timeout) throws IOException, GeneralSecurityException;
    
    /**
     * Find registered IPFS content for a given address.
     */
    List<FHandle> findIpfsContent(Address owner, Long timeout) throws IOException;
    
    /**
     * Send content to a target address via IPFS. 
     */
    FHandle sendIpfsContent(Address owner, Multihash cid, Address target, Long timeout) throws IOException, GeneralSecurityException;

    /**
     * Unregister a IPFS content.
     */
    List<Multihash> unregisterIpfsContent(Address owner, List<Multihash> cids) throws IOException;
    
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
}
