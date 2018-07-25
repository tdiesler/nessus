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

import java.math.BigDecimal;

import io.nessus.AbstractNetwork;
import io.nessus.Block;
import io.nessus.Blockchain;
import io.nessus.Network;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BitcoinNetwork extends AbstractNetwork implements Network {

    public BitcoinNetwork(Blockchain blockchain, BitcoindRpcClient client) {
        super(blockchain, client);
    }

    @Override
    public BigDecimal estimateFee() {
        return new BigDecimal("0.001");
    }

    @Override
    public Integer getBlockRate() {
        return 600;
    }

    /**
     * 546 satoshis at the default rate of 3000 sat/kB.
     * 
     * @see https://github.com/tdiesler/bitcoin/blob/master/src/policy/policy.cpp#L18
     */
    @Override
    public BigDecimal getDustThreshold() {
        return new BigDecimal("0.00000546");
    }

    @Override
    public Block getBlock(String blockHash) {
        return new BitcoinBlock(client.getBlock(blockHash));
    }
}
