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
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Blockchain;
import io.nessus.ipfs.ContentManager;
import io.nessus.ipfs.IPFSClient;
import io.nessus.ipfs.core.DefaultContentManager;
import io.nessus.utils.AssertArgument;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;

public class JAXRSApplication extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(JAXRSApplication.class);

    private static final String implVersion;
    private static final String implBuild;
    
    static {
        try (InputStream ins = JAXRSApplication.class.getResourceAsStream("/" + JarFile.MANIFEST_NAME)) {
            Manifest manifest = new Manifest(ins);
            Attributes attribs = manifest.getMainAttributes();
            implVersion = attribs.getValue("Implementation-Version");
            implBuild = attribs.getValue("Implementation-Build");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static JAXRSConfig config;
    private static JAXRSServer jaxrsServer;

    static JAXRSApplication INSTANCE;
    final ContentManager cntManager;

    public JAXRSApplication() throws Exception {
        this(config);
    }
        
    public JAXRSApplication(JAXRSConfig config) throws Exception {
        AssertArgument.assertNotNull(config,  "Null config");
        
        String buildNumber = getImplBuild() != null ? "Build: " + getImplBuild() : "";
        LOG.info("Nessus Version: {} {}", getImplVersion(), buildNumber);
        
        Blockchain blockchain = config.getBlockchain();
        JAXRSClient.logBlogchainNetworkAvailable(blockchain.getNetwork());
        
        IPFSClient ipfsClient = config.getIPFSClient();
        LOG.info("IPFS PeerId: {}",  ipfsClient.getPeerId());
        LOG.info("IPFS Address: {}",  ipfsClient.getAPIAddress());
        LOG.info("IPFS Version: {}",  ipfsClient.version());
        
        cntManager = new DefaultContentManager(ipfsClient, blockchain, config);
        LOG.info("DefaultContentManager{}", config);

        INSTANCE = this;
    }

    static String getImplVersion() {
		return implVersion;
	}

	static String getImplBuild() {
		boolean snapshot = implVersion != null && implVersion.endsWith("SNAPSHOT");
		return snapshot ? implBuild : null;
	}

    public static JAXRSServer serverStart(JAXRSConfig config) throws Exception {
        
        JAXRSSanityCheck.verifyPlatform();
        JAXRSApplication.config = config;

        Builder builder = Undertow.builder().addHttpListener(config.jaxrsPort, config.jaxrsHost);
        UndertowJaxrsServer undertowServer = new UndertowJaxrsServer().start(builder);
        
        String ctxpath = config.jaxrsPath;
        undertowServer.deploy(JAXRSApplication.class, ctxpath);

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

    @Override
    public Set<Object> getSingletons() {
        HashSet<Object> singletons = new HashSet<Object>();
        Collections.addAll(singletons, cntManager);
        return singletons;
    }

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(JAXRSResource.class);
        classes.add(RuntimeExceptionMapper.class);
        classes.add(GeneralSecurityExceptionMapper.class);
        classes.add(IOExceptionMapper.class);
        return classes;
    }

    public static class JAXRSServer {

        final JAXRSConfig options;
        final UndertowJaxrsServer server;

        JAXRSServer(UndertowJaxrsServer server, JAXRSConfig options) {
            this.options = options;
            this.server = server;
        }

        public URL getRootURL() {
            try {
                return new URL(String.format("http://%s:%d/nessus", options.jaxrsHost, options.jaxrsPort));
            } catch (MalformedURLException ex) {
                throw new IllegalStateException(ex);
            }
        }

        public void stop() {
            server.stop();
        }
    }

    @Provider
    public static class IOExceptionMapper extends AbstractExceptionMapper<IOException> {
    }

    @Provider
    public static class GeneralSecurityExceptionMapper extends AbstractExceptionMapper<GeneralSecurityException> {
    }

    @Provider
    public static class RuntimeExceptionMapper extends AbstractExceptionMapper<RuntimeException> {
    }

    public static class AbstractExceptionMapper<T extends Exception> implements ExceptionMapper<T> {

        @Override
        public Response toResponse(T ex) {
            StringWriter strwr = new StringWriter();
            ex.printStackTrace(new PrintWriter(strwr));
            LOG.error("ERROR executing request", ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(strwr.toString()).build();
        }
    }
}
