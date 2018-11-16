package io.nessus.ipfs.jaxrs;

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
import java.io.InputStream;
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
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.BlockchainFactory;
import io.nessus.RpcClientSupport;
import io.nessus.bitcoin.BitcoinBlockchain;
import io.nessus.core.ipfs.ContentManager;
import io.nessus.core.ipfs.IPFSClient;
import io.nessus.core.ipfs.impl.DefaultContentManager;
import io.nessus.core.ipfs.impl.DefaultIPFSClient;
import io.nessus.utils.AssertState;
import io.nessus.utils.StreamUtils;
import io.nessus.utils.SystemUtils;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

@ApplicationPath("/nessus")
public class JAXRSApplication extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(JAXRSApplication.class);

    static final JAXRSConfig config;
    static {
        int port = PortProvider.getPort();
        String host = PortProvider.getHost();
        config = new JAXRSConfig(host, port);
    }

    private static JAXRSApplication INSTANCE;
    private static JAXRSServer jaxrsServer;

    private final ContentManager contentManager;

    public static void main(String[] args) throws Exception {

        JAXRSSanityCheck.verifyPlatform();

        try {
            JAXRSApplication.mainInternal(args);
        } catch (Throwable th) {
            Runtime.getRuntime().exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    public static JAXRSServer serverStart() throws Exception {

        String className = SystemUtils.getenv(BlockchainFactory.BLOCKCHAIN_CLASS_NAME, BitcoinBlockchain.class.getName());
        ClassLoader classLoader = JAXRSApplication.class.getClassLoader();
        Class<Blockchain> bcclass = (Class<Blockchain>) classLoader.loadClass(className);
        
        Blockchain blockchain = BlockchainFactory.getBlockchain(rpcUrl(), bcclass);
        String networkName = blockchain.getNetwork().getClass().getSimpleName();
        BitcoindRpcClient rpcclient = ((RpcClientSupport) blockchain).getRpcClient();
        LOG.info("{} Version: {}",  networkName, rpcclient.getNetworkInfo().version());

        IPFSClient ipfs = new DefaultIPFSClient();
        LOG.info("IPFS Version: {}",  ipfs.version());

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
        IPFSClient ipfs = new DefaultIPFSClient();
        contentManager = new DefaultContentManager(ipfs, blockchain);

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

    public static URL rpcUrl() {
        URL rpcUrl;
        String urlstr = SystemUtils.getenv(Constants.ENV_JSONRPC_URL, null);
        String user = SystemUtils.getenv(Constants.ENV_JSONRPC_USER, null);
        String pass = SystemUtils.getenv(Constants.ENV_JSONRPC_PASS, null);
        if (urlstr != null) {
            try {
                rpcUrl = new URL(String.format("http://%s", urlstr));
                String userInfo = rpcUrl.getUserInfo();
                if (userInfo == null && user != null && pass != null) {
                    String protocol = rpcUrl.getProtocol();
                    String host = rpcUrl.getHost();
                    int port = rpcUrl.getPort();
                    String path = rpcUrl.getPath();
                    rpcUrl = new URL(String.format("%s://%s:%s@%s:%d%s", protocol, user, pass, host, port, path));
                }
            } catch (MalformedURLException ex) {
                throw new IllegalStateException(ex);
            }
        } else {
            rpcUrl = BitcoinJSONRPCClient.DEFAULT_JSONRPC_REGTEST_URL;
        }
        return rpcUrl;
    }

    // Entry point with no system exit
    private static void mainInternal(String[] args) throws Exception {

        Options options = new Options();
        CmdLineParser parser = new CmdLineParser(options);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException ex) {
            helpScreen(parser);
            throw ex;
        }

        try {
            JAXRSApplication.process(parser, options);
        } catch (Throwable th) {
            LOG.error("Error executing command", th);
            throw th;
        }
    }

    private static void process(CmdLineParser parser, Options options) throws Exception {

        if (options.args.contains("start")) {
            serverStart();
        }

        else if (options.args.contains("stop")) {
            serverStop();
            System.exit(0);
        }

        else {
            helpScreen(parser);
        }
    }

    private static void helpScreen(CmdLineParser cmdParser) {
        InputStream readme = JAXRSApplication.class.getResourceAsStream("/README.md");
        AssertState.assertNotNull(readme, "Cannot obtain README.md");
        try {
            StreamUtils.copyStream(readme, System.out);
        } catch (IOException e) {
            // ignore
        }
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
