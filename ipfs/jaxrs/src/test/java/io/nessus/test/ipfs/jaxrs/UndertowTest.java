package io.nessus.test.ipfs.jaxrs;

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

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Application;

import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.test.TestPortProvider;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class UndertowTest {

    private static UndertowJaxrsServer server;

    @BeforeClass
    public static void init() throws Exception {
        server = new UndertowJaxrsServer().start();
    }

    @AfterClass
    public static void stop() throws Exception {
        server.stop();
    }

    @Test
    public void testApplicationPath() throws Exception {
        server.deploy(MyApp.class);
        Client client = ClientBuilder.newClient();
        String val = client.target(TestPortProvider.generateURL("/base/test")).request().get(String.class);
        Assert.assertEquals("hello world", val);
        client.close();
    }

    @Path("/test")
    public static class Resource {
        @GET
        @Produces("text/plain")
        public String get() {
            return "hello world";
        }
    }

    @ApplicationPath("/base")
    public static class MyApp extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            HashSet<Class<?>> classes = new HashSet<Class<?>>();
            classes.add(Resource.class);
            return classes;
        }
    }
}
