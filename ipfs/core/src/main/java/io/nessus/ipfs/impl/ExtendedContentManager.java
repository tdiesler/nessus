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
import java.io.Reader;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import io.nessus.UTXO;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.FHandle;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class ExtendedContentManager extends DefaultContentManager {

    public ExtendedContentManager(Config config) {
        super(config);
    }

    @Override
    public Config getConfig() {
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
    public FHeaderId getFHeaderId() {
        return super.getFHeaderId();
    }

    @Override
    public FHandle decrypt(FHandle fhandle, Address owner, Path destPath, boolean storePlain) throws IOException, GeneralSecurityException {
        return super.decrypt(fhandle, owner, destPath, storePlain);
    }

    @Override
    public PublicKey getPubKeyFromTx(UTXO utxo, Address owner) {
        return super.getPubKeyFromTx(utxo, owner);
    }

    @Override
    public FHandle getFHandleFromTx(UTXO utxo, Address owner) {
        return super.getFHandleFromTx(utxo, owner);
    }

    @Override
    public FHeader readFHeader(Reader rd) throws IOException {
        return super.readFHeader(rd);
    }

}
