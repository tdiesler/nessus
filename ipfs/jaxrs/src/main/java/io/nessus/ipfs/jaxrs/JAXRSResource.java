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
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Network;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.ContentManager;
import io.nessus.ipfs.FHandle;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;

public class JAXRSResource implements JAXRSEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(JAXRSResource.class);

    final ContentManager cntmgr;
    
    public JAXRSResource() throws IOException {

        JAXRSApplication app = JAXRSApplication.getInstance();
        cntmgr = app.getCntManager();
    }

    @Override
    public String registerAddress(String addr) throws GeneralSecurityException {

        assertBlockchainNetworkAvailable();
        
        Address owner = assertWalletAddress(addr);
        PublicKey pubKey = cntmgr.registerAddress(owner);
        String encKey = Base64.getEncoder().encodeToString(pubKey.getEncoded());
        
        LOG.info("/regaddr {} => {}", owner, encKey);

        return encKey;
    }

    @Override
    public String findAddressRegistation(String addr) {

        assertBlockchainNetworkAvailable();
        
        Address owner = assertWalletAddress(addr);
        PublicKey pubKey = cntmgr.findAddressRegistation(owner);
        String encKey = pubKey != null ? Base64.getEncoder().encodeToString(pubKey.getEncoded()) : null;
        
        LOG.info("/findkey {} => {}", owner, encKey);

        return encKey;
    }

    @Override
    public String unregisterAddress(String addr) {

        assertBlockchainNetworkAvailable();
        
        Address owner = assertWalletAddress(addr);
        PublicKey pubKey = cntmgr.unregisterAddress(owner);
        String encKey = Base64.getEncoder().encodeToString(pubKey.getEncoded());
        
        LOG.info("/rmaddr {} => {}", owner, encKey);

        return encKey;
    }

    @Override
    public SFHandle addIpfsContent(String addr, String path, URL srcUrl) throws IOException, GeneralSecurityException {
        AssertArgument.assertTrue(path != null || srcUrl != null, "Path or URL must be given");

        assertBlockchainNetworkAvailable();
        
        Address owner = assertWalletAddress(addr);
        FHandle fhandle;
        if (srcUrl != null) {
            fhandle = cntmgr.addIpfsContent(owner, Paths.get(path), srcUrl);
        } else {
            fhandle = cntmgr.addIpfsContent(owner, Paths.get(path));
        }

        AssertState.assertTrue(fhandle.getFilePath().toFile().exists());
        AssertState.assertNotNull(fhandle.getCid());

        SFHandle shandle = new SFHandle(fhandle);
        LOG.info("/addipfs {}", shandle);

        return shandle;
    }

    @Override
    public SFHandle addIpfsContent(String addr, String path, InputStream input) throws IOException, GeneralSecurityException {

        assertBlockchainNetworkAvailable();
        
        Address owner = assertWalletAddress(addr);
        FHandle fhandle = cntmgr.addIpfsContent(owner, Paths.get(path), input);

        AssertState.assertTrue(fhandle.getFilePath().toFile().exists());
        AssertState.assertNotNull(fhandle.getCid());

        SFHandle shandle = new SFHandle(fhandle);
        LOG.info("/addipfs {}", shandle);

        return shandle;
    }

    @Override
    public SFHandle getIpfsContent(String addr, String cid, String path, Long timeout) throws IOException, GeneralSecurityException {

        assertBlockchainNetworkAvailable();
        
        Address owner = assertWalletAddress(addr);
        FHandle fhandle = cntmgr.getIpfsContent(owner, cid, Paths.get(path), timeout);

        AssertState.assertTrue(fhandle.getFilePath().toFile().exists());
        AssertState.assertNull(fhandle.getCid());

        SFHandle shandle = new SFHandle(fhandle);
        LOG.info("/getipfs {} => {}", cid, shandle);

        return shandle;
    }

    @Override
    public SFHandle sendIpfsContent(String addr, String cid, @QueryParam("target") String rawTarget, Long timeout) throws IOException, GeneralSecurityException {

        assertBlockchainNetworkAvailable();
        
        Address owner = assertWalletAddress(addr);
        Address target = assertWalletAddress(rawTarget);

        FHandle fhandle = cntmgr.sendIpfsContent(owner, cid, target, timeout);
        AssertState.assertNotNull(fhandle.getCid());

        SFHandle shandle = new SFHandle(fhandle);
        LOG.info("/sendipfs {} => {}", cid, shandle);

        return shandle;
    }

    @Override
    public List<SFHandle> findIpfsContent(String addr, Long timeout) throws IOException {

        assertBlockchainNetworkAvailable();
        
        List<SFHandle> result = new ArrayList<>();

        Address owner = assertWalletAddress(addr);
        for (FHandle fh : cntmgr.findIpfsContent(owner, timeout)) {
            result.add(new SFHandle(fh));
        }
        LOG.info("/findipfs {} => {} files", addr, result.size());

        return result;
    }

    @Override
    public List<String> unregisterIpfsContent(String addr, List<String> cids) throws IOException {
        
        assertBlockchainNetworkAvailable();
        
        Address owner = assertWalletAddress(addr);
        
        List<String> result = cntmgr.unregisterIpfsContent(owner, cids);
        LOG.info("/rmipfs {} {} => {}", addr, cids, result);

        return result;
    }

    @Override
    public List<SFHandle> findLocalContent(String addr, String path) throws IOException {

        assertBlockchainNetworkAvailable();
        
        List<SFHandle> result = new ArrayList<>();

        Address owner = assertWalletAddress(addr);
        if (path == null) {
            for (FHandle fhres : cntmgr.findLocalContent(owner)) {
                result.add(new SFHandle(fhres));
            }
            LOG.info("/findlocal {} => {} files", addr, result.size());
        } else {
            FHandle fhres = cntmgr.findLocalContent(owner, Paths.get(path));
            if (fhres != null) result.add(new SFHandle(fhres));
            LOG.info("/findlocal {} {} => {}", addr, path, fhres != null ? fhres.toString(true) : null);
        }

        return result;
    }

    @Override
    public InputStream getLocalContent(String addr, String path) throws IOException {

        assertBlockchainNetworkAvailable();
        
        Address owner = assertWalletAddress(addr);
        InputStream content = cntmgr.getLocalContent(owner, Paths.get(path));
        LOG.info("/getlocal {} {}", addr, path);

        return content;
    }

    @Override
    public boolean removeLocalContent(String addr, String path) throws IOException {

        assertBlockchainNetworkAvailable();
        
        Address owner = assertWalletAddress(addr);
        boolean removed = cntmgr.removeLocalContent(owner, Paths.get(path));
        LOG.info("/rmlocal {} {} => {}", addr, path, removed);

        return removed;
    }

    private void assertBlockchainNetworkAvailable() {
        Network network = cntmgr.getBlockchain().getNetwork();
        JAXRSClient.assertBlockchainNetworkAvailable(network);
    }

    private Address assertWalletAddress(String rawaddr) {
        Address addr = cntmgr.getBlockchain().getWallet().findAddress(rawaddr);
        AssertState.assertNotNull(addr, "Unknown address: " + addr);
        return addr;
    }
}
