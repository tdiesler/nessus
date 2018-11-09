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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.utils.AssertState;
import io.nessus.utils.SystemUtils;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BlockchainFactory {

    public static final String RPC_CLIENT_CLASS_NAME = "RPC_CLIENT_CLASS_NAME";
    public static final String BLOCKCHAIN_CLASS_NAME = "BLOCKCHAIN_CLASS_NAME";

    static final Logger LOG = LoggerFactory.getLogger(BlockchainFactory.class);
    
    private static Blockchain INSTANCE;
    
    public static <T extends Blockchain> T getBlockchain(Properties props, Class<T> bcClass) throws Exception {
        String rpcuser = props.getProperty("rpcuser");
        String rpcpass = props.getProperty("rpcpassword");
        String rpchost = props.getProperty("rpcconnect");
        String rpcport = props.getProperty("rpcport");
        return getBlockchain(new URL(String.format("http://%s:%s@%s:%s", rpcuser, rpcpass, rpchost, rpcport)), bcClass);
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends Blockchain> T getBlockchain(URL rpcUrl, Class<T> bcClass) {
        if (INSTANCE == null) {
            try {
                LOG.info("{}: {}", bcClass.getSimpleName(), getLogURL(rpcUrl));
                BitcoindRpcClient client = (BitcoindRpcClient) loadRpcClientClass().getConstructor(URL.class).newInstance(rpcUrl);
                INSTANCE = (Blockchain) bcClass.getConstructor(BitcoindRpcClient.class).newInstance(client);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return (T) INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Blockchain> T getBlockchain(Class<T> bcClass) {
        AssertState.assertNotNull(INSTANCE, "Blockchain not initialized");
        return (T) INSTANCE;
    }
    
    public static Blockchain getBlockchain() {
        return getBlockchain(Blockchain.class);
    }
    
    private static Class<?> loadRpcClientClass() throws ClassNotFoundException {
        String className = SystemUtils.getenv(RPC_CLIENT_CLASS_NAME, "wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient");
        ClassLoader loader = BlockchainFactory.class.getClassLoader();
        return loader.loadClass(className);
    }
    
    private static URL getLogURL(URL rpcUrl) throws MalformedURLException {
        String protocol = rpcUrl.getProtocol();
        String host = rpcUrl.getHost();
        int port = rpcUrl.getPort();
        String path = rpcUrl.getPath();
        String userpass = "";
        if (rpcUrl.getUserInfo() != null) {
            userpass = rpcUrl.getUserInfo();
            userpass = userpass.substring(0, userpass.indexOf(':')) + ":*******@";
        }
        String query = rpcUrl.getQuery() != null ? rpcUrl.getQuery() : "";
        return new URL(String.format("%s://%s%s:%d%s%s", protocol, userpass, host, port, path, query));
    }
}
