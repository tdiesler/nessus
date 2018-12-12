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
import org.junit.Assert;
import org.junit.BeforeClass;

import io.nessus.AbstractWallet;
import io.nessus.Blockchain;
import io.nessus.Network;
import io.nessus.UTXO;
import io.nessus.Wallet;
import io.nessus.Wallet.Address;
import io.nessus.ipfs.Config;
import io.nessus.ipfs.Config.ConfigBuilder;
import io.nessus.ipfs.FHandle;
import io.nessus.ipfs.core.ExtendedContentManager;
import io.nessus.testing.AbstractBlockchainTest;

public abstract class AbstractJAXRSTest extends AbstractBlockchainTest {

    protected static ExtendedContentManager cntmgr;
    protected static Blockchain blockchain;
    protected static Network network;
    protected static Wallet wallet;

    protected static Address addrBob;
    protected static Address addrMary;

    @BeforeClass
    public static void beforeClass() throws Exception {

        Config config = new ConfigBuilder()
        		.bcurl(DEFAULT_JSONRPC_REGTEST_URL)
        		.build();
        
        cntmgr = new ExtendedContentManager(config);
        
        blockchain = cntmgr.getBlockchain();
        network = blockchain.getNetwork();
        wallet = blockchain.getWallet();

        importAddresses(wallet, AbstractJAXRSTest.class);

        addrBob = wallet.getAddress(LABEL_BOB);
        addrMary = wallet.getAddress(LABEL_MARY);
    }

    @AfterClass
    public static void afterClass() throws Exception {

    	AbstractWallet absw = (AbstractWallet) wallet;
    	absw.redeemChange(LABEL_BOB, addrBob);
    	absw.redeemChange(LABEL_MARY, addrMary);
    }

    protected void redeemLockedUtxos(Address addr) {

        // Unlock all UTXOs
        wallet.listLockUnspent(Arrays.asList(addr))
            .forEach(utxo -> wallet.lockUnspent(utxo, true));

        // Redeem all locked UTXOs
        String label = addr.getLabels().get(0);
        List<UTXO> utxos = wallet.listUnspent(Arrays.asList(addr));
        String changeAddr = wallet.getChangeAddress(label).getAddress();
        
        wallet.sendToAddress(addr.getAddress(), changeAddr, Wallet.ALL_FUNDS, utxos);
    }

    FHandle findIpfsContent(Address addr, String cid, Long timeout) throws Exception {
        
        List<FHandle> fhandles = cntmgr.findIpfsContent(addr, timeout);
        FHandle fhandle  = fhandles.stream().filter(fh -> fh.getCid().equals(cid)).findFirst().get();
        Assert.assertNotNull(fhandle);
        
        return fhandle;
    }
}
