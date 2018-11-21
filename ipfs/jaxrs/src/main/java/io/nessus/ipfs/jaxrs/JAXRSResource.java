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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Wallet.Address;
import io.nessus.core.ipfs.ContentManager;
import io.nessus.core.ipfs.FHandle;
import io.nessus.utils.AssertState;

public class JAXRSResource implements JAXRSEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(JAXRSResource.class);

    final ContentManager cntmgr;

    public JAXRSResource() throws IOException {

        JAXRSApplication app = JAXRSApplication.getInstance();
        cntmgr = app.getContentManager();
    }

    @Override
    public SFHandle add(String rawAddr, String path, InputStream input) throws IOException, GeneralSecurityException {

        Address owner = assertWalletAddress(rawAddr);
        FHandle fhandle = cntmgr.add(owner, input, Paths.get(path));

        AssertState.assertTrue(new File(fhandle.getURL().getPath()).exists());
        AssertState.assertNotNull(fhandle.getCid());

        SFHandle shandle = new SFHandle(fhandle);
        LOG.info("/add => {}", shandle);

        return shandle;
    }

    @Override
    public String findAddressRegistation(String rawAddr) {

        Address addr = assertWalletAddress(rawAddr);
        PublicKey pubKey = cntmgr.findAddressRegistation(addr);

        String encKey = pubKey != null ? Base64.getEncoder().encodeToString(pubKey.getEncoded()) : null;
        LOG.info("/findkey => {}", encKey);

        return encKey;
    }

    @Override
    public List<SFHandle> findIPFSContent(String rawAddr, Long timeout) throws IOException {

        List<SFHandle> result = new ArrayList<>();

        Address owner = assertWalletAddress(rawAddr);
        for (FHandle fhandle : cntmgr.findIPFSContent(owner, timeout)) {
            result.add(new SFHandle(fhandle));
        }
        LOG.info("/findipfs => {}", result);

        return result;
    }

    @Override
    public List<SFHandle> findLocalContent(String rawAddr) throws IOException {

        List<SFHandle> result = new ArrayList<>();

        Address owner = assertWalletAddress(rawAddr);
        for (FHandle fhandle : cntmgr.findLocalContent(owner)) {
            result.add(new SFHandle(fhandle));
        }
        LOG.info("/findlocal => {}", result);

        return result;
    }

    @Override
    public SFHandle get(String rawAddr, String cid, String path, Long timeout) throws IOException, GeneralSecurityException {

        Address owner = assertWalletAddress(rawAddr);
        FHandle fhandle = cntmgr.get(owner, cid, Paths.get(path), timeout);

        AssertState.assertTrue(new File(fhandle.getURL().getPath()).exists());
        AssertState.assertNull(fhandle.getCid());

        SFHandle shandle = new SFHandle(fhandle);
        LOG.info("/get => {}", shandle);

        return shandle;
    }

    @Override
    public InputStream getLocalContent(String rawAddr, String path) throws IOException {

        Address owner = assertWalletAddress(rawAddr);
        InputStream content = cntmgr.getLocalContent(owner, Paths.get(path));
        LOG.info("/getlocal => {}", content);

        return content;
    }

    @Override
    public String registerAddress(String rawAddr) throws GeneralSecurityException {

        Address addr = assertWalletAddress(rawAddr);

        PublicKey pubKey = cntmgr.registerAddress(addr);
        LOG.info("/register => {}", pubKey);

        String encKey = Base64.getEncoder().encodeToString(pubKey.getEncoded());
        return encKey;
    }

    @Override
    public boolean removeLocalContent(String rawAddr, String path) throws IOException {

        Address owner = assertWalletAddress(rawAddr);
        boolean deleted = cntmgr.removeLocalContent(owner, Paths.get(path));
        LOG.info("/dellocal => {}", deleted);

        return deleted;
    }

    @Override
    public SFHandle send(String rawAddr, String cid, @QueryParam("target") String rawTarget, Long timeout) throws IOException, GeneralSecurityException {

        Address owner = assertWalletAddress(rawAddr);
        Address target = assertWalletAddress(rawTarget);

        FHandle fhandle = cntmgr.send(owner, cid, target, timeout);
        AssertState.assertNotNull(fhandle.getCid());

        SFHandle shandle = new SFHandle(fhandle);
        LOG.info("/send => {}", shandle);

        return shandle;
    }

    @Override
    public String unregisterAddress(String rawAddr) {

        Address addr = assertWalletAddress(rawAddr);
        PublicKey pubKey = cntmgr.unregisterAddress(addr);
        LOG.info("/unregaddr => {}", pubKey);

        String encKey = Base64.getEncoder().encodeToString(pubKey.getEncoded());
        return encKey;
    }

    @Override
    public List<String> unregisterIPFSContent(String rawAddr, List<String> cids) throws IOException {
        
        Address owner = assertWalletAddress(rawAddr);
        
        List<String> result = cntmgr.unregisterIPFSContent(owner, cids);
        LOG.info("/unregipfs => {}", result);

        return result;
    }

    private Address assertWalletAddress(String rawAddr) {
        Address addr = cntmgr.getBlockchain().getWallet().findAddress(rawAddr);
        AssertState.assertNotNull(addr, "Unknown address: " + rawAddr);
        return addr;
    }
}
