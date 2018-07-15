package io.nessus.bitcoin;

/*-
 * #%L
 * Nessus :: Bitcoin
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.Network;
import io.nessus.Wallet;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinBlockchain extends BitcoinClientSupport implements Blockchain {

    static final Logger LOG = LoggerFactory.getLogger(BitcoinBlockchain.class);

    private Wallet wallet;
    private Network network;
    
    public BitcoinBlockchain(BitcoindRpcClient client) {
        super(client);
    }
    
    @Override
    public Wallet getWallet() {
        if (wallet == null) {
            wallet = createWallet();
        }
        return wallet;
    }

    @Override
    public Network getNetwork() {
        if (network == null) {
            network = createNetwork();
        }
        return network;
    }
    
    protected Wallet createWallet() {
        return new BitcoinWallet(this, getRpcClient());
    }

    protected Network createNetwork() {
        return new BitcoinNetwork(this, getRpcClient());
    }
}
