package io.nessus.ipfs.impl;

/*-
 * #%L
 * Nessus :: IPFS :: Core
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
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import io.nessus.UTXO;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.FHandle;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class ExtendedContentManager extends DefaultContentManager {

    public ExtendedContentManager(ContentManagerConfig config) {
        super(config);
    }

    @Override
    public ContentManagerConfig getConfig() {
        return super.getConfig();
    }

    @Override
    public Path getRootPath() {
        return super.getRootPath();
    }

    @Override
    public Path getPlainPath(Address owner) {
        return super.getPlainPath(owner);
    }

    @Override
    public Path getCryptPath(Address owner) {
        return super.getCryptPath(owner);
    }

    @Override
    public void clearFileCache() {
        super.clearFileCache();
    }

    @Override
    public BitcoindRpcClient getRpcClient() {
        return super.getRpcClient();
    }

    @Override
    public FHeaderValues getFHeaderValues() {
        return super.getFHeaderValues();
    }

    @Override
    public FHandle decrypt(FHandle fhandle, Path destPath, boolean storePlain) throws IOException, GeneralSecurityException {
        return super.decrypt(fhandle, destPath, storePlain);
    }

    @Override
    public PublicKey getPubKeyFromTx(UTXO utxo, Address owner) {
        return super.getPubKeyFromTx(utxo, owner);
    }

    @Override
    public FHandle getFHandleFromTx(Address owner, UTXO utxo) {
        return super.getFHandleFromTx(owner, utxo);
    }

    @Override
    public FHandle buildTreeFromPath(Address owner, Path path) throws IOException {
        return super.buildTreeFromPath(owner, path);
    }
}
