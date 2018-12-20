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

/**
 * Encryption
 * ----------
 * 
 * An RSA key pair is derived from the Bob's private key.
 * The public RSA key is registered on the blockchain.
 * The RSA key pair is indempotent for the same user.
 * 
 * An AES secret is derived from Bob's private key and the content id.
 * The AES secret is encrypted with Mary's public RSA key, which results in an encryption token.
 * The encryption token is recorded in the ipfs file header.
 * An AES initialization vector (IV) is derived from Bob's private key and the content id.
 * The file content as encrypted with the AES secret and the deterministic IV.
 * The encrypted file (which consists of header and body) is added to IPFS.
 * The resulting IPFS content id (CID) is recorded on the blockchain.
 * The CID for the encrypted content is indempotent for the same user and content.
 * 
 * Decryption
 * ----------
 * 
 * Mary obtains an IPFS content id from her unspent transaction outputs (UTXO).
 * An IPFS get operation fetches the encrypted file (i.e. header + body)
 * Mary obtains the encryption token from the file header.
 * The encryption token is decrypted with the Mary's private RSA key, which results in the AES secret.
 * Mary decrypts the file body with the so obtained AES secret.
 * The plain content is stored in Mary's workspace.
 * 
 * KEEP IN SYNC WITH IpfsWorkflowTest
 */
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
