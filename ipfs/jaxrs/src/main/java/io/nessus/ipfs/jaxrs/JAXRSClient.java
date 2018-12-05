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
import java.net.URI;
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

public class JAXRSClient implements JAXRSEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(JAXRSClient.class);

    final Client client = ClientBuilder.newClient();
    final URI urlRoot;

    static Long networkVersion;
    
    public JAXRSClient(URI urlRoot) {
        this.urlRoot = urlRoot;
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
    public String registerAddress(String rawAddr) throws IOException {

        WebTarget target = client.target(generateURL("/regaddr"))
                .queryParam("addr", rawAddr);

        Response res = processResponse(target.request().get(Response.class));
        String encKey = res.readEntity(String.class);
        
        LOG.info("/regaddr {} => {}", rawAddr, encKey);

        return encKey;
    }

    @Override
    public String findAddressRegistation(String rawAddr) throws IOException {

        WebTarget target = client.target(generateURL("/findkey"))
                .queryParam("addr", rawAddr);

        Response res = processResponse(target.request().get(Response.class));
        if (Status.NO_CONTENT.getStatusCode() == res.getStatus()) return null;

        String encKey = res.readEntity(String.class);
        LOG.info("/findkey {} => {}", rawAddr, encKey);

        return encKey;
    }

    @Override
    public String unregisterAddress(String rawAddr) throws IOException {

        WebTarget target = client.target(generateURL("/rmaddr"))
                .queryParam("addr", rawAddr);

        Response res = processResponse(target.request().get(Response.class));
        String encKey = res.readEntity(String.class);
        
        LOG.info("/rmaddr {} => {}", rawAddr, encKey);

        return encKey;
    }

    @Override
    public SFHandle addIpfsContent(String rawAddr, String relPath, URL srcUrl) throws IOException {
        AssertArgument.assertTrue(relPath != null || srcUrl != null, "Path or URL must be given");

        WebTarget target = client.target(generateURL("/addipfs"))
                .queryParam("addr", rawAddr);
        
        if (relPath != null) target = target.queryParam("path", relPath);
        if (srcUrl != null) target = target.queryParam("url", srcUrl);

        Response res = processResponse(target.request().get(Response.class));

        SFHandle shandle = res.readEntity(SFHandle.class);
        LOG.info("/addipfs => {}", shandle.toString(true));

        return shandle;
    }

    @Override
    public SFHandle addIpfsContent(String rawAddr, String relPath, InputStream input) throws IOException {

        WebTarget target = client.target(generateURL("/addipfs"))
                .queryParam("addr", rawAddr)
                .queryParam("path", relPath);

        Response res = processResponse(target.request().post(Entity.entity(input, MediaType.APPLICATION_OCTET_STREAM), Response.class));

        SFHandle shandle = res.readEntity(SFHandle.class);
        LOG.info("/addipfs => {}", shandle);

        return shandle;
    }

    @Override
    public SFHandle getIpfsContent(String rawAddr, String cid, String relPath, Long timeout) throws IOException {

        WebTarget target = client.target(generateURL("/getipfs"))
                .queryParam("addr", rawAddr)
                .queryParam("path", relPath)
                .queryParam("timeout", timeout)
                .queryParam("cid", cid);

        Response res = processResponse(target.request().get(Response.class));

        SFHandle shandle = res.readEntity(SFHandle.class);
        LOG.info("/getipfs {} => {}", cid, shandle.toString(true));

        return shandle;
    }

    @Override
    public SFHandle sendIpfsContent(String rawAddr, String cid, String rawTarget, Long timeout) throws IOException {

        WebTarget target = client.target(generateURL("/sendipfs"))
                .queryParam("addr", rawAddr)
                .queryParam("target", rawTarget)
                .queryParam("cid", cid)
                .queryParam("timeout", timeout);

        Response res = processResponse(target.request().get(Response.class));

        SFHandle shandle = res.readEntity(SFHandle.class);
        LOG.info("/sendipfs {} => {}", cid, shandle.toString(true));

        return shandle;
    }

    @Override
    public List<SFHandle> findIpfsContent(String rawAddr, Long timeout) throws IOException {

        WebTarget target = client.target(generateURL("/findipfs"))
                .queryParam("addr", rawAddr)
                .queryParam("timeout", timeout);

        Response res = processResponse(target.request().get(Response.class));

        List<SFHandle> result = Arrays.asList(res.readEntity(SFHandle[].class));
        LOG.info("/findipfs {} => {} files", rawAddr, result.size());

        return result;
    }

    @Override
    public List<String> unregisterIpfsContent(String rawAddr, List<String> cids) throws IOException {

        WebTarget target = client.target(generateURL("/rmipfs"))
                .queryParam("addr", rawAddr)
                .queryParam("cid", cids.toArray());

        Response res = processResponse(target.request().get(Response.class));

        List<String> result = Arrays.asList(res.readEntity(String[].class));
        LOG.info("/rmipfs {} {} => {}", rawAddr, cids, result);

        return result;
    }

    @Override
    public List<SFHandle> findLocalContent(String rawAddr, String path) throws IOException {

        WebTarget target = client.target(generateURL("/findlocal"))
                .queryParam("addr", rawAddr);

        if (path != null) target = target.queryParam("path", path);
        
        Response res = processResponse(target.request().get(Response.class));

        List<SFHandle> result = Arrays.asList(res.readEntity(SFHandle[].class));
        LOG.info("/findlocal {} => {} files", rawAddr, result.size());

        return result;
    }

    @Override
    public InputStream getLocalContent(String rawAddr, String path) throws IOException {

        WebTarget target = client.target(generateURL("/getlocal"))
                .queryParam("addr", rawAddr)
                .queryParam("path", path);

        Response res = processResponse(target.request().get(Response.class));

        InputStream content = res.readEntity(InputStream.class);
        LOG.info("/getlocal {} {}", rawAddr, path);

        return content;
    }

    @Override
    public boolean removeLocalContent(String rawAddr, String path) throws IOException {

        WebTarget target = client.target(generateURL("/rmlocal"))
                .queryParam("addr", rawAddr)
                .queryParam("path", path);

        Response res = processResponse(target.request().get(Response.class));

        Boolean removed = res.readEntity(Boolean.class);
        LOG.info("/rmlocal {} {} => {}", rawAddr, path, removed);

        return removed;
    }

    private Response processResponse(Response res) throws IOException {
        Status status = Status.fromStatusCode(res.getStatus());
        if (status == Status.INTERNAL_SERVER_ERROR) {

            String stackTrace = res.readEntity(String.class);
            LOG.error(stackTrace);

            String line = new BufferedReader(new StringReader(stackTrace)).readLine();
            
            String message, errorType;
            int colIdx = line.indexOf(':');
            if (colIdx > 0) {
                errorType = line.substring(0, colIdx);
                message = line.substring(colIdx + 2);
            } else {
                errorType = line;
                message = "";
            } 

            if (NessusUserFault.class.getName().equals(errorType)) {
                
                throw new NessusUserFault(message);
                
            } else if (NessusException.class.getName().equals(errorType)) {
                
                throw new NessusException(message);
                
            } else {
                
                throw new IllegalStateException(message);
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
        return urlRoot + path;
    }
}
