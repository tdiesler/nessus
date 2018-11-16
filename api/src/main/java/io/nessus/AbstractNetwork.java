package io.nessus;

/*-
 * #%L
 * Nessus :: API
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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Wallet.Address;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.NetworkInfo;

public abstract class AbstractNetwork extends RpcClientSupport implements Network {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    final Blockchain blockchain;
    
    protected AbstractNetwork(Blockchain blockchain, BitcoindRpcClient client) {
        super(client);
        this.blockchain = blockchain;
    }

    @Override
    public String getBlockHash(Integer height) {
        return client.getBlockHash(height);
    }

    @Override
    public Integer getBlockCount() {
        return client.getBlockCount();
    }

    @Override
    public NetworkInfo getNetworkInfo() {
        return client.getNetworkInfo();
    }

    @Override
    public List<String> generate(int numBlocks) {
        return generate(numBlocks, null);
    }

    @Override
    public List<String> generate(int numBlocks, Address address) {
        if (address != null) {
            return client.generateToAddress(numBlocks, address.getAddress());
        } else {
            return client.generate(numBlocks);
        }
    }
}
