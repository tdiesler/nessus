package io.nessus.ipfs.jaxrs;

/*-
 * #%L
 * Nessus :: IPFS :: JAXRS
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
import java.security.GeneralSecurityException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/")
public interface JaxrsEndpoint {

    /**
     * Register the given address with the system.
     * 
     * This records a data transaction on the blockchain which published the 
     * public IPFS content encryption key for the given address. 
     * 
     * The cost for this transaction is 10 x dust for the configured network, 
     * which is paid to the given address and hence creating an a new unspent
     * transaction output (UTXO). This UTXO is then locked to prevent accidetal
     * spending.
     * 
     * Example:
     * 
     *      curl http://192.168.178.20:8081/nessus/regaddr?addr=mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB
     *      
     *      {
     *        "label": "Bob",
     *        "address": "mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB",
     *        "balance": 49.99992040,
     *        "encKey": "MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAExY/IQsYFnzFS8/QdFyCU5RNK7OkK79Fu7hvOKearR+4=",
     *        "watchOnly": false
     *      }
     * 
     * @return A handle to the registered address
     */
    @GET
    @Path("/regaddr")
    @Produces(MediaType.APPLICATION_JSON)
    SAHandle registerAddress(@QueryParam("addr") String addr) throws GeneralSecurityException, IOException;

    /**
     * Get address registration details.
     * 
     * Example:
     * 
     *      curl http://192.168.178.20:8081/nessus/addrinfo?label=Bob
     *      
     *      [{
     *        "label": "Bob",
     *        "address": "mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB",
     *        "balance": 49.99992040,
     *        "encKey": "MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAExY/IQsYFnzFS8/QdFyCU5RNK7OkK79Fu7hvOKearR+4=",
     *        "watchOnly": false
     *      }]
     * 
     *       
     * @param label An optional filter for a specific label
     * @param addr An optional filter for a specific address 
     * @return A list of address handles
     */
    @GET
    @Path("/addrinfo")
    @Produces(MediaType.APPLICATION_JSON)
    List<SAHandle> findAddressInfo(@QueryParam("label") String label, @QueryParam("addr") String addr) throws IOException;

    /**
     * Unegister the given address from the system.
     * 
     * The UTXO that holds the registrion for the given address is unlocked 
     * and then spent to the given address. The system no longer sees the 
     * transaction that holds the public IPFS content encryption key.
     * 
     * Example:
     * 
     *      curl http://192.168.178.20:8081/nessus/rmaddr?addr=mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB
     *      
     *      {
     *        "label": "Bob",
     *        "address": "mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB",
     *        "balance": 49.99992040,
     *        "encKey": "MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAExY/IQsYFnzFS8/QdFyCU5RNK7OkK79Fu7hvOKearR+4=",
     *        "watchOnly": false
     *      }
     *  
     * @return A handle to the unregistered address
     */
    @GET
    @Path("/rmaddr")
    @Produces(MediaType.APPLICATION_JSON)
    SAHandle unregisterAddress(@QueryParam("addr") String addr) throws IOException;
    
    /**
     * Add IPFS content from the given input stream.
     * 
     * The owner address must have been registered with the system
     * so that the content can be properly encrypted before it is added
     * to IPFS.
     * 
     * Encrypted content must be given a unique path in the owner's namespace.
     * 
     * Example:
     * 
     *      echo "Hello World" > test.txt
     *      curl --request POST --data @test.txt "http://192.168.178.20:8081/nessus/addipfs?addr=mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB&path=test.txt"
     *      
     *      {
     *        "cid": "Qme7mLoshqb8E8szmebWqiY8o1UbMAYcWL1b3YdTyPv6Ds",
     *        "owner": "mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB",
     *        "txId": "2fd2bd081623130306074f326df86aa10042d7d9e482e13e2e9ee3570fe30ab0",
     *        "path": "test.txt",
     *        "encrypted": true,
     *        "available": true,
     *        ...
     *      } 
     *       
     * @return A handle to the encrypted IPFS content
     */
    @POST
    @Path("/addipfs")
    @Produces(MediaType.APPLICATION_JSON)
    SFHandle addIpfsContent(@QueryParam("addr") String owner, @QueryParam("path") String path, InputStream input) throws IOException, GeneralSecurityException;

    /**
     * Add IPFS content from the given path or URL.
     * 
     * The owner address must have been registered with the system
     * so that the content can be properly encrypted before it is added
     * to IPFS.
     * 
     * Encrypted content must be given a unique path in the owner's namespace.
     * 
     * The URL parameter is optional. If not given the path is interpreted as a relative path
     * in the owner's namespace. It can point to a single file or a directory, in which case the
     * whole document tree is encrypted and added to IPFS.
     * 
     * URL Example:
     * 
     *      curl "http://192.168.178.20:8081/nessus/addipfs?addr=mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB&path=userfile.txt&url=https://raw.githubusercontent.com/jboss-fuse/nessus/master/ipfs/jaxrs/src/test/resources/userfile.txt"
     *      
     *      {
     *        "cid": "QmUVjhRvnvrXtu5KtBRdLp1zjG5jwMJEjBV6Efag6cw69T",
     *        "owner": "mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB",
     *        "txId": "65f63ad78d5798dd29f259f913ed8659c484c6fab57e0b9bc9bfa694d1eef5eb",
     *        "path": "userfile.txt",
     *        "encrypted": true,
     *        "available": true,
     *        ...
     *      } 
     *       
     * Path Example:
     * 
     *      curl "http://192.168.178.20:8081/nessus/addipfs?addr=mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB&path=contentA"
     *      
     *      {
     *        "cid": "QmNpjzWvd7ZqkeqitGCXYvLHvyNQZ6SPJhU9uG9NFKX8ug",
     *        "owner": "mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB",
     *        "txId": "5c408ffcad460e8b3940f5cfc4f479aa0d703a86f105fefc285e9d57b849a19b",
     *        "path": "contentA",
     *        "encrypted": true,
     *        "available": true,
     *        ...
     *        "children": [{...}]
     *      } 
     *       
     * @return A handle to the encrypted IPFS content
     */
    @GET
    @Path("/addipfs")
    @Produces(MediaType.APPLICATION_JSON)
    SFHandle addIpfsContent(@QueryParam("addr") String owner, @QueryParam("path") String path, @QueryParam("url") URL srcURL) throws IOException, GeneralSecurityException;

    /**
     * Get IPFS content by id.
     * 
     * The owner address must have been registered with the system
     * so that the content can be properly decrypted before it is stored.
     * 
     * Decrypted content must be given a unique path in the owner's namespace.
     * 
     * Example:
     * 
     *      curl "http://192.168.178.20:8081/nessus/getipfs?addr=mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB&path=foo.txt&cid=Qme7mLoshqb8E8szmebWqiY8o1UbMAYcWL1b3YdTyPv6Ds"
     *      
     *      {
     *        "owner": "mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB",
     *        "path": "foo.txt",
     *        "encrypted": false,
     *        "available": true,
     *        ...
     *      } 
     *       
     * @param timeout The optional timeout in miliseconds that IPFS is given to find the content on the network. 
     * @return A handle to the decrypted local content
     */
    @GET
    @Path("/getipfs")
    @Produces(MediaType.APPLICATION_JSON)
    SFHandle getIpfsContent(@QueryParam("addr") String owner, @QueryParam("cid") String cid, @QueryParam("path") String path, @QueryParam("timeout") Long timeout) throws IOException, GeneralSecurityException;

    /**
     * Send IPFS content to a target address.
     * 
     * The owner and the target address must have been registered with the system
     * so that the content can be properly decrypted before it is stored.
     * 
     * The system first gets the encrypted content from IPFS, then decrypts it with the owner's 
     * private key, then encrypts it again with the receipient's public key and finally registeres
     * a DATA transaction on the blockchain that records the content transfer.
     * 
     * Example:
     *      
     *      ipfs cat Qme7mLoshqb8E8szmebWqiY8o1UbMAYcWL1b3YdTyPv6Ds
     *      curl http://192.168.178.20:8081/nessus/findkey?addr=mm2PoHeFncAStYeZJSSTa4bmUVXRa3L6PL
     *      curl "http://192.168.178.20:8081/nessus/sendipfs?addr=mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB&target=mm2PoHeFncAStYeZJSSTa4bmUVXRa3L6PL&cid=Qme7mLoshqb8E8szmebWqiY8o1UbMAYcWL1b3YdTyPv6Ds"
     *      
     *      {
     *        "cid": "QmZWCV3c75ShzyuGaJ38yFQWv3zPDvzAAb4g8oygh7AGDH",
     *        "owner": "mm2PoHeFncAStYeZJSSTa4bmUVXRa3L6PL",
     *        "txId": "08675978946c793460c530b20de93b4ce94640e614bded56976c5ffe954b5aee",
     *        "path": "test.txt",
     *        "encrypted": true,
     *        "available": true,
     *        ...
     *      } 
     *       
     * @param timeout The optional timeout in miliseconds that IPFS is given to find the content on the network. 
     * @return A handle to the encrypted IPFS content
     */
    @GET
    @Path("/sendipfs")
    @Produces(MediaType.APPLICATION_JSON)
    SFHandle sendIpfsContent(@QueryParam("addr") String owner, @QueryParam("cid") String cid, @QueryParam("target") String target, @QueryParam("timeout") Long timeout) throws IOException, GeneralSecurityException;

    /**
     * Find IPFS content for a given owner address.
     * 
     * The owner must have been registered with the system.
     * 
     * The system finds IPFS content registrations based on unspent transaction outputs.
     * 
     * Example:
     *      
     *      curl "http://192.168.178.20:8081/nessus/findipfs?addr=mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB"
     *      
     *      {
     *        "cid": "QmNpjzWvd7ZqkeqitGCXYvLHvyNQZ6SPJhU9uG9NFKX8ug",
     *        "owner": "mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB",
     *        "txId": "5c408ffcad460e8b3940f5cfc4f479aa0d703a86f105fefc285e9d57b849a19b",
     *        "path": "contentA",
     *        "encrypted": true,
     *        "available": true,
     *        ...
     *      } 
     *       
     * @param timeout The optional timeout in miliseconds that IPFS is given to find the content on the network. 
     * @return A handle to the encrypted IPFS content
     */
    @GET
    @Path("/findipfs")
    @Produces(MediaType.APPLICATION_JSON)
    List<SFHandle> findIpfsContent(@QueryParam("addr") String owner, @QueryParam("timeout") Long timeout) throws IOException;

    /**
     * Unregister IPFS content.
     * 
     * Similarly to "unregister address", this spends the UTXO for the IPFS
     * content registration.
     * 
     * Example:
     *      
     *      curl "http://192.168.178.20:8081/nessus/rmipfs?addr=mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB&cid=QmUVjhRvnvrXtu5KtBRdLp1zjG5jwMJEjBV6Efag6cw69T"
     *      
     *      ["QmUVjhRvnvrXtu5KtBRdLp1zjG5jwMJEjBV6Efag6cw69T"]
     *       
     * @return An array of unregistered content ids
     */
    @GET
    @Path("/rmipfs")
    @Produces(MediaType.APPLICATION_JSON)
    List<String> unregisterIpfsContent(@QueryParam("addr") String owner, @QueryParam("cid") List<String> cids) throws IOException;
    
    /**
     * Find local content for a given owner.
     * 
     * Example:
     *      
     *      curl "http://192.168.178.20:8081/nessus/findlocal?addr=mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB"
     *      
     *      {
     *        "owner": "mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB",
     *        "path": "foo.txt",
     *        "encrypted": false,
     *        "available": true,
     *        ...
     *      } 
     *       
     * @param path An optional conent path 
     * @return A list of handles to decrypted local content
     */
    @GET
    @Path("/findlocal")
    @Produces(MediaType.APPLICATION_JSON)
    List<SFHandle> findLocalContent(@QueryParam("addr") String owner, @QueryParam("path") String path) throws IOException;

    /**
     * Gets local content for a given owner.
     * 
     * Example:
     *      
     *      curl "http://192.168.178.20:8081/nessus/getlocal?addr=mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB&path=foo.txt"
     *      
     *      Hello World 
     */
    @GET
    @Path("/getlocal")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    InputStream getLocalContent(@QueryParam("addr") String owner, @QueryParam("path") String path) throws IOException;

    /**
     * Remove local content for a given owner.
     * 
     * Example:
     *      
     *      curl "http://192.168.178.20:8081/nessus/rmlocal?addr=mt5CNtbvx9qSxCRze5AqTDdsr4CZCn9MQB&path=foo.txt"
     */
    @GET
    @Path("/rmlocal")
    @Produces(MediaType.TEXT_PLAIN)
    boolean removeLocalContent(@QueryParam("addr") String owner, @QueryParam("path") String path) throws IOException;
}
