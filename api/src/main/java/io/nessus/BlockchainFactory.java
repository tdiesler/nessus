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
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class BlockchainFactory {

    public static final String RPC_CLIENT_CLASS_NAME = "RPC_CLIENT_CLASS_NAME";
    public static final String BLOCKCHAIN_CLASS_NAME = "BLOCKCHAIN_CLASS_NAME";

    static final Logger LOG = LoggerFactory.getLogger(BlockchainFactory.class);
    
    private static Blockchain INSTANCE;
    
    public static Blockchain getBlockchain(Properties props) throws Exception {
        String rpcuser = props.getProperty("rpcuser");
        String rpcpass = props.getProperty("rpcpassword");
        String rpchost = props.getProperty("rpcconnect");
        String rpcport = props.getProperty("rpcport");
        String className = props.getProperty(Blockchain.class.getName());
        Class<? extends Blockchain> clazz = loadBlockchainClass(className);
        return getBlockchain(new URL(String.format("http://%s:%s@%s:%s", rpcuser, rpcpass, rpchost, rpcport)), clazz);
    }
    
    public static Blockchain getBlockchain(URL rpcUrl) {
        if (INSTANCE == null) {
            try {
                INSTANCE = getBlockchain(rpcUrl, loadBlockchainClass());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return INSTANCE;
    }

    public static Blockchain getBlockchain(URL rpcUrl, Class<? extends Blockchain> bcclass) {
        if (INSTANCE == null) {
            try {
                LOG.info("{}: {}", bcclass.getSimpleName(), getLogURL(rpcUrl));
                BitcoindRpcClient client = (BitcoindRpcClient) loadRpcClientClass().getConstructor(URL.class).newInstance(rpcUrl);
                INSTANCE = (Blockchain) bcclass.getConstructor(BitcoindRpcClient.class).newInstance(client);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return INSTANCE;
    }

    public static Blockchain getBlockchain() {
        AssertState.assertNotNull(INSTANCE, "Blockchain PRC client not initialized");
        return INSTANCE;
    }
    
    private static Class<?> loadRpcClientClass() throws ClassNotFoundException {
        String className = System.getenv(RPC_CLIENT_CLASS_NAME);
        ClassLoader loader = BlockchainFactory.class.getClassLoader();
        return loader.loadClass(className != null ? className : "wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient");
    }
    
    private static Class<? extends Blockchain> loadBlockchainClass() throws ClassNotFoundException {
        String className = System.getenv(BLOCKCHAIN_CLASS_NAME);
        return loadBlockchainClass(className);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Blockchain> loadBlockchainClass(String className) throws ClassNotFoundException {
        if (className == null) className = "io.nessus.bitcoin.BitcoinBlockchain";
        ClassLoader loader = BlockchainFactory.class.getClassLoader();
        return (Class<? extends Blockchain>) loader.loadClass(className);
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
