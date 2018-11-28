package io.nessus.ipfs.jaxrs;

import static io.nessus.ipfs.jaxrs.JAXRSConstants.ENV_NESSUS_JAXRS_ADDR;
import static io.nessus.ipfs.jaxrs.JAXRSConstants.ENV_NESSUS_JAXRS_PORT;

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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.PortProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.bitcoin.BitcoinBlockchain;
import io.nessus.ipfs.ContentManager;
import io.nessus.ipfs.ContentManager.Config;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.impl.DefaultContentManager;
import io.nessus.ipfs.impl.DefaultIPFSClient;
import io.nessus.utils.SystemUtils;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;

@ApplicationPath("/nessus")
public class JAXRSApplication extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(JAXRSApplication.class);

    static final JAXRSConfig config;
    static {
        String jaxrsAddr = SystemUtils.getenv(ENV_NESSUS_JAXRS_ADDR, PortProvider.getHost());
        int jaxrsPort = Integer.parseInt(SystemUtils.getenv(ENV_NESSUS_JAXRS_PORT, "" + PortProvider.getPort()));
        config = new JAXRSConfig(jaxrsAddr, jaxrsPort);
    }

    private final ContentManager contentManager;

    private static JAXRSApplication INSTANCE;
    private static JAXRSServer jaxrsServer;

    public static void main(String[] args) throws Exception {

        JAXRSSanityCheck.verifyPlatform();

        try {
            serverStart();
        } catch (Throwable th) {
            LOG.error("Error executing command", th);
            Runtime.getRuntime().exit(1);
        }
    }

    public static JAXRSServer serverStart() throws Exception {
        
        IPFSClient ipfsClient = ipfsClient();
        LOG.info("IPFS Address: {}",  ipfsClient.getAPIAddress());
        LOG.info("IPFS Version: {}",  ipfsClient.version());

        URL rpcUrl = blockchainURL();
        Class<Blockchain> bcclass = blockchainClass();
        Blockchain blockchain = BlockchainFactory.getBlockchain(rpcUrl, bcclass);
        JAXRSClient.logBlogchainNetworkAvailable(blockchain.getNetwork());
        
        Builder builder = Undertow.builder().addHttpListener(config.port, config.host);
        UndertowJaxrsServer undertowServer = new UndertowJaxrsServer().start(builder);
        undertowServer.deploy(JAXRSApplication.class);

        jaxrsServer = new JAXRSServer(undertowServer, config);
        LOG.info("Nessus JAXRS: {}",  jaxrsServer.getRootURL());

        return jaxrsServer;
    }

    public static void serverStop() {

        if (jaxrsServer != null) {
            jaxrsServer.stop();
            jaxrsServer = null;
        }
    }

    static JAXRSApplication getInstance() {
        return INSTANCE;
    }

    public JAXRSApplication() {
        ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
        providerFactory.registerProvider(GeneralSecurityExceptionMapper.class);
        providerFactory.registerProvider(RuntimeExceptionMapper.class);
        providerFactory.registerProvider(IOExceptionMapper.class);

        Blockchain blockchain = BlockchainFactory.getBlockchain();
        IPFSClient ipfsClient = new DefaultIPFSClient();
        
        Config config = new Config(blockchain, ipfsClient);
        contentManager = new DefaultContentManager(config);

        INSTANCE = this;
    }

    public ContentManager getContentManager() {
        return contentManager;
    }

    @Override
    public Set<Object> getSingletons() {
        HashSet<Object> singletons = new HashSet<Object>();
        Collections.addAll(singletons, config, contentManager);
        return singletons;
    }

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(JAXRSResource.class);
        return classes;
    }

    @SuppressWarnings("unchecked")
    public static Class<Blockchain> blockchainClass() throws ClassNotFoundException {
        String className = SystemUtils.getenv(BlockchainFactory.BLOCKCHAIN_CLASS_NAME, BitcoinBlockchain.class.getName());
        ClassLoader classLoader = JAXRSApplication.class.getClassLoader();
        return (Class<Blockchain>) classLoader.loadClass(className);
    }

    public static URL blockchainURL() {
        URL rpcUrl;
        String rpcaddr = SystemUtils.getenv(JAXRSConstants.ENV_BLOCKCHAIN_JSONRPC_ADDR, null);
        String rpcport = SystemUtils.getenv(JAXRSConstants.ENV_BLOCKCHAIN_JSONRPC_PORT, null);
        String rpcuser = SystemUtils.getenv(JAXRSConstants.ENV_BLOCKCHAIN_JSONRPC_USER, null);
        String rpcpass = SystemUtils.getenv(JAXRSConstants.ENV_BLOCKCHAIN_JSONRPC_PASS, null);
        if (rpcaddr != null && rpcport != null) {
            try {
                rpcUrl = new URL(String.format("http://%s:%s", rpcaddr, rpcport));
                String userInfo = rpcUrl.getUserInfo();
                if (userInfo == null) {
                    rpcUrl = new URL(String.format("http://%s:%s@%s:%s", rpcuser, rpcpass, rpcaddr, rpcport));
                }
            } catch (MalformedURLException ex) {
                throw new IllegalStateException(ex);
            }
        } else {
            rpcUrl = BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;
        }
        return rpcUrl;
    }

    public static IPFSClient ipfsClient() {
        String rpcaddr = SystemUtils.getenv(IPFSClient.ENV_IPFS_JSONRPC_ADDR, null);
        String rpcport = SystemUtils.getenv(IPFSClient.ENV_IPFS_JSONRPC_PORT, null);
        Integer port = rpcport != null ? new Integer(rpcport) : null;
        return new DefaultIPFSClient(rpcaddr, port);
    }

    public static class JAXRSServer {

        final UndertowJaxrsServer server;
        final JAXRSConfig config;

        JAXRSServer(UndertowJaxrsServer server, JAXRSConfig config) {
            this.server = server;
            this.config = config;
        }

        public URL getRootURL() {
            try {
                return new URL(String.format("http://%s:%d/nessus", config.host, config.port));
            } catch (MalformedURLException ex) {
                throw new IllegalStateException(ex);
            }
        }

        public JAXRSConfig getJAXRSConfig() {
            return config;
        }

        public void stop() {
            server.stop();
        }
    }

    public static class JAXRSConfig {

        final String host;
        final int port;

        JAXRSConfig(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
        
        public String toString() {
            return String.format("http://%s:%d", host, port);
        }
    }

    @Provider
    public static class GeneralSecurityExceptionMapper extends AbstractExceptionMapper<GeneralSecurityException> {
    }

    @Provider
    public static class IOExceptionMapper extends AbstractExceptionMapper<IOException> {
    }

    @Provider
    public static class RuntimeExceptionMapper extends AbstractExceptionMapper<RuntimeException> {
    }

    static class AbstractExceptionMapper<T extends Exception> implements ExceptionMapper<T> {

        @Override
        public Response toResponse(T rte) {
            StringWriter strwr = new StringWriter();
            rte.printStackTrace(new PrintWriter(strwr));
            LOG.error("ERROR executing request", rte);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(strwr.toString()).build();
        }
    }
}
