package io.nessus.test.ipfs.jaxrs;

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

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import io.nessus.AbstractWallet;
import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.Network;
import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.bitcoin.BitcoinBlockchain;
import io.nessus.core.ipfs.ContentManager;
import io.nessus.core.ipfs.IPFSClient;
import io.nessus.core.ipfs.impl.DefaultContentManager;
import io.nessus.core.ipfs.impl.DefaultIPFSClient;
import io.nessus.testing.AbstractBlockchainTest;

public abstract class AbstractJAXRSTest extends AbstractBlockchainTest {

    protected static ContentManager cntmgr;
    protected static Blockchain blockchain;
    protected static Network network;
    protected static AbstractWallet wallet;

    protected static Address addrBob;
    protected static Address addrMary;

    @BeforeClass
    public static void beforeClass() throws Exception {

        blockchain = BlockchainFactory.getBlockchain(DEFAULT_JSONRPC_REGTEST_URL, BitcoinBlockchain.class);
        wallet = (AbstractWallet) blockchain.getWallet();
        network = blockchain.getNetwork();

        IPFSClient ipfs = new DefaultIPFSClient();
        cntmgr = new DefaultContentManager(ipfs, blockchain);

        importAddresses(wallet, AbstractJAXRSTest.class);

        addrBob = wallet.getAddress(LABEL_BOB);
        addrMary = wallet.getAddress(LABEL_MARY);
    }

    @AfterClass
    public static void afterClass() throws Exception {

        wallet.redeemChange(LABEL_BOB, addrBob);
        wallet.redeemChange(LABEL_MARY, addrMary);
    }

    protected void redeemLockedUtxos(String label, Address addr) {

        // Unlock all UTXOs
        wallet.listLockUnspent(Arrays.asList(addr))
            .forEach(utxo -> wallet.lockUnspent(utxo, true));

        // Redeem all locked UTXOs
        List<UTXO> utxos = wallet.listUnspent(label);
        String changeAddr = wallet.getChangeAddress(label).getAddress();
        wallet.sendToAddress(addr.getAddress(), changeAddr, Wallet.ALL_FUNDS, utxos);
    }
}
