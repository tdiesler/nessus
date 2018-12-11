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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.Network;
import io.nessus.ipfs.NessusException;
import io.nessus.ipfs.NessusUserFault;
import io.nessus.utils.AssertArgument;
import io.nessus.utils.AssertState;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;

public class JaxrsClient implements JaxrsEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(JaxrsClient.class);

    final Client client = ClientBuilder.newClient();
    final URL jaxrsUrl;

    static Long networkVersion;
    
    public JaxrsClient(URL jaxrsUrl) {
        this.jaxrsUrl = jaxrsUrl;
    }

    public static void logBlogchainNetworkAvailable(Network network) {
        try {
            assertBlockchainNetworkAvailable(network);
        } catch (BitcoinRPCException rte) {
            // ignore
        }
    }

    public static void assertBlockchainNetworkAvailable(Network network) {
        if (networkVersion == null) {
            try {
                String networkName = network.getClass().getSimpleName();
                networkVersion = network.getNetworkInfo().version();
                LOG.info("{} Version: {}",  networkName, networkVersion);
            } catch (BitcoinRPCException rte) {
                String errmsg = rte.getRPCError().getMessage();
                LOG.error("Blockchain not available: {}", errmsg);
                throw rte;
            } catch (RuntimeException rte) {
                LOG.error("Blockchain error", rte);
                throw rte;
            }
        }
    }

    @Override
    public AddrHandle registerAddress(String addr) throws IOException {

        WebTarget target = client.target(generateURL("/regaddr"))
                .queryParam("addr", addr);

        Response res = processResponse(target.request().get(Response.class));
        AddrHandle ahandle = res.readEntity(AddrHandle.class);
        
        LOG.info("/regaddr {} => {}", addr, ahandle);

        return ahandle;
    }

    @Override
    public List<AddrHandle> findAddressInfo(String label, String addr) throws IOException {

        WebTarget target = client.target(generateURL("/addrinfo"));
        if (label != null) target = target.queryParam("label", label);
        if (addr != null) target = target.queryParam("addr", addr);

        Response res = processResponse(target.request().get(Response.class));
        List<AddrHandle> result = Arrays.asList(res.readEntity(AddrHandle[].class));

        LOG.info("/addrinfo {} {} => {}", label, addr, result);

        return result;
    }

    @Override
    public AddrHandle unregisterAddress(String addr) throws IOException {

        WebTarget target = client.target(generateURL("/rmaddr"))
                .queryParam("addr", addr);

        Response res = processResponse(target.request().get(Response.class));
        if (Status.NO_CONTENT.getStatusCode() == res.getStatus()) return null;

        AddrHandle ahandle = res.readEntity(AddrHandle.class);
        LOG.info("/rmaddr {} => {}", addr, ahandle);

        return ahandle;
    }

    @Override
    public SFHandle addIpfsContent(String addr, String relPath, URL srcUrl) throws IOException {
        AssertArgument.assertTrue(relPath != null || srcUrl != null, "Path or URL must be given");

        WebTarget target = client.target(generateURL("/addipfs"))
                .queryParam("addr", addr);
        
        if (relPath != null) target = target.queryParam("path", relPath);
        if (srcUrl != null) target = target.queryParam("url", srcUrl);

        Response res = processResponse(target.request().get(Response.class));

        SFHandle shandle = res.readEntity(SFHandle.class);
        LOG.info("/addipfs => {}", shandle.toString(true));

        return shandle;
    }

    @Override
    public SFHandle addIpfsContent(String addr, String relPath, InputStream input) throws IOException {

        WebTarget target = client.target(generateURL("/addipfs"))
                .queryParam("addr", addr)
                .queryParam("path", relPath);

        Response res = processResponse(target.request().post(Entity.entity(input, MediaType.APPLICATION_OCTET_STREAM), Response.class));

        SFHandle shandle = res.readEntity(SFHandle.class);
        LOG.info("/addipfs => {}", shandle);

        return shandle;
    }

    @Override
    public SFHandle getIpfsContent(String addr, String cid, String relPath, Long timeout) throws IOException {

        WebTarget target = client.target(generateURL("/getipfs"))
                .queryParam("addr", addr)
                .queryParam("path", relPath)
                .queryParam("timeout", timeout)
                .queryParam("cid", cid);

        Response res = processResponse(target.request().get(Response.class));

        SFHandle shandle = res.readEntity(SFHandle.class);
        LOG.info("/getipfs {} => {}", cid, shandle.toString(true));

        return shandle;
    }

    @Override
    public SFHandle sendIpfsContent(String addr, String cid, String rawTarget, Long timeout) throws IOException {

        WebTarget target = client.target(generateURL("/sendipfs"))
                .queryParam("addr", addr)
                .queryParam("target", rawTarget)
                .queryParam("cid", cid)
                .queryParam("timeout", timeout);

        Response res = processResponse(target.request().get(Response.class));

        SFHandle shandle = res.readEntity(SFHandle.class);
        LOG.info("/sendipfs {} => {}", cid, shandle.toString(true));

        return shandle;
    }

    @Override
    public List<SFHandle> findIpfsContent(String addr, Long timeout) throws IOException {

        WebTarget target = client.target(generateURL("/findipfs"))
                .queryParam("addr", addr)
                .queryParam("timeout", timeout);

        Response res = processResponse(target.request().get(Response.class));

        List<SFHandle> result = Arrays.asList(res.readEntity(SFHandle[].class));
        LOG.info("/findipfs {} => {} files", addr, result.size());

        return result;
    }

    @Override
    public List<String> unregisterIpfsContent(String addr, List<String> cids) throws IOException {

        WebTarget target = client.target(generateURL("/rmipfs"))
                .queryParam("addr", addr)
                .queryParam("cid", cids.toArray());

        Response res = processResponse(target.request().get(Response.class));

        List<String> result = Arrays.asList(res.readEntity(String[].class));
        LOG.info("/rmipfs {} {} => {}", addr, cids, result);

        return result;
    }

    @Override
    public List<SFHandle> findLocalContent(String addr, String path) throws IOException {

        WebTarget target = client.target(generateURL("/findlocal"))
                .queryParam("addr", addr);

        if (path != null) target = target.queryParam("path", path);
        
        Response res = processResponse(target.request().get(Response.class));

        List<SFHandle> result = Arrays.asList(res.readEntity(SFHandle[].class));
        LOG.info("/findlocal {} => {} files", addr, result.size());

        return result;
    }

    @Override
    public InputStream getLocalContent(String addr, String path) throws IOException {

        WebTarget target = client.target(generateURL("/getlocal"))
                .queryParam("addr", addr)
                .queryParam("path", path);

        Response res = processResponse(target.request().get(Response.class));

        InputStream content = res.readEntity(InputStream.class);
        LOG.info("/getlocal {} {}", addr, path);

        return content;
    }

    @Override
    public boolean removeLocalContent(String addr, String path) throws IOException {

        WebTarget target = client.target(generateURL("/rmlocal"))
                .queryParam("addr", addr)
                .queryParam("path", path);

        Response res = processResponse(target.request().get(Response.class));

        Boolean removed = res.readEntity(Boolean.class);
        LOG.info("/rmlocal {} {} => {}", addr, path, removed);

        return removed;
    }

    private Response processResponse(Response res) throws IOException {
        Status status = Status.fromStatusCode(res.getStatus());
        if (status == Status.INTERNAL_SERVER_ERROR) {

            String stackTrace = res.readEntity(String.class);
            LOG.error(stackTrace);

            String line = new BufferedReader(new StringReader(stackTrace)).readLine();
            
            String errMessage, errType;
            int colIdx = line.indexOf(':');
            if (colIdx > 0) {
                errType = line.substring(0, colIdx);
                errMessage = line.substring(colIdx + 2);
            } else {
                errType = line;
                errMessage = "";
            } 

            if (NessusUserFault.class.getName().equals(errType)) {
                
                throw new NessusUserFault(errMessage);
                
            } else if (NessusException.class.getName().equals(errType)) {
                
                throw new NessusException(errMessage);
                
            } else if (IOException.class.getName().equals(errType)) {
                
                throw new IOException(errMessage);
                
            } else {
                
                Throwable errInst = null;
                try {
                    ClassLoader loader = JaxrsClient.class.getClassLoader();
                    Class<?> extype = loader.loadClass(errType);
                    Constructor<?> ctor = extype.getConstructor(String.class);
                    errInst = (Throwable) ctor.newInstance(errMessage);
                } catch (Exception ex) {
                    LOG.error("Cannot load server error: " + line);
                }
                
                if (errInst instanceof RuntimeException) {
                    throw (RuntimeException) errInst;
                }

                if (errInst != null) {
                    throw new IllegalStateException(errInst);
                }
                
                throw new IllegalStateException(errMessage);
            }

        } else if (status == Status.NO_CONTENT) {

            // ignore;

        } else {
            
            if (status != Status.OK) LOG.error("{} - {}", status.getStatusCode(), status.getReasonPhrase());
            AssertState.assertEquals(Status.OK, status, status.getReasonPhrase());
        }
        
        return res;
    }

    private String generateURL(String path) {
        return jaxrsUrl + path;
    }
}
