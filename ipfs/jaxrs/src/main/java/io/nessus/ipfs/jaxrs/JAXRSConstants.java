package io.nessus.ipfs.jaxrs;

import io.nessus.core.ipfs.IPFSClient;

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

public class JAXRSConstants {

    public static final String ENV_IPFS_JSONRPC_ADDR = IPFSClient.ENV_IPFS_JSONRPC_ADDR;
    public static final String ENV_IPFS_JSONRPC_PORT = IPFSClient.ENV_IPFS_JSONRPC_PORT;
    
    public static final String ENV_IPFS_GATEWAY_ADDR = IPFSClient.ENV_IPFS_GATEWAY_ADDR;
    public static final String ENV_IPFS_GATEWAY_PORT = IPFSClient.ENV_IPFS_GATEWAY_PORT;

    public static final String ENV_BLOCKCHAIN_JSONRPC_ADDR = "BLOCKCHAIN_JSONRPC_ADDR";
    public static final String ENV_BLOCKCHAIN_JSONRPC_PORT = "BLOCKCHAIN_JSONRPC_PORT";
    public static final String ENV_BLOCKCHAIN_JSONRPC_USER = "BLOCKCHAIN_JSONRPC_USER";
    public static final String ENV_BLOCKCHAIN_JSONRPC_PASS = "BLOCKCHAIN_JSONRPC_PASS";

    public static final String ENV_NESSUS_JAXRS_ADDR = "NESSUS_JAXRS_ADDR";
    public static final String ENV_NESSUS_JAXRS_PORT = "NESSUS_JAXRS_PORT";
}
