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

import java.util.Map;

import io.nessus.AbstractBlockchain;
import io.nessus.AbstractNetwork;
import io.nessus.AbstractWallet;
import io.nessus.Blockchain;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinBlockchain extends AbstractBlockchain implements Blockchain {

    private Boolean pruned;
    
    public BitcoinBlockchain(BitcoindRpcClient client) {
        super(client);
    }
    
    @Override
    protected AbstractWallet createWallet() {
        return new BitcoinWallet(this, getRpcClient());
    }

    @Override
    protected AbstractNetwork createNetwork() {
        return new BitcoinNetwork(this, getRpcClient());
    }

    @Override
    public boolean isPruned() {
        if (pruned == null) {
            @SuppressWarnings("unchecked")
            Map<String, ?> resmap = (Map<String, ?>) ((BitcoinJSONRPCClient) client).query("getblockchaininfo");
            Object value = resmap.get("pruned");
            pruned = value != null ? Boolean.parseBoolean(value.toString()) : false;
        }
        return pruned;
    }
}
