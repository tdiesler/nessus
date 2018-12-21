package io.nessus;

import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;

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

import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public abstract class RpcClientSupport {

    protected final BitcoindRpcClient client;
    
    public RpcClientSupport(BitcoindRpcClient client) {
        this.client = client;
    }
    
    public BitcoindRpcClient getRpcClient() {
        return client;
    }

	public Object query(String method, Object... args) {
		return ((BitcoinJSONRPCClient) client).query(method, args);
	}
}
