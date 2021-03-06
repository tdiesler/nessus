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

import static wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.testing.AbstractBlockchainTest;

public abstract class AbstractBitcoinTest extends AbstractBlockchainTest {

    @Override
	protected Blockchain createBlockchain() {
        return BlockchainFactory.getBlockchain(DEFAULT_JSONRPC_REGTEST_URL, BitcoinBlockchain.class);
	}

	@Override
	protected BitcoinBlockchain getBlockchain() {
		return (BitcoinBlockchain) super.getBlockchain();
	}
}
