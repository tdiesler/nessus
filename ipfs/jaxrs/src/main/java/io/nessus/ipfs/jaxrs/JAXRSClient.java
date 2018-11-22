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
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nessus.utils.AssertState;

public class JAXRSClient implements JAXRSEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(JAXRSClient.class);

    final Client client = ClientBuilder.newClient();
    final URI urlRoot;

    public JAXRSClient(URI urlRoot) {
        this.urlRoot = urlRoot;
    }

    @Override
    public String registerAddress(String rawAddr) throws IOException {

        WebTarget target = client.target(generateURL("/register"))
                .queryParam("addr", rawAddr);

        Response res = processResponse(target.request().get(Response.class));

        String encKey = res.readEntity(String.class);
        LOG.info("/register => {}", encKey);

        return encKey;
    }

    @Override
    public String unregisterAddress(String rawAddr) throws IOException {

        WebTarget target = client.target(generateURL("/rmaddr"))
                .queryParam("addr", rawAddr);

        Response res = processResponse(target.request().get(Response.class));
        String encKey = res.readEntity(String.class);
        LOG.info("/rmaddr => {}", encKey);

        return encKey;
    }

    @Override
    public SFHandle add(String rawAddr, String relPath, InputStream input) throws IOException {

        WebTarget target = client.target(generateURL("/add"))
                .queryParam("addr", rawAddr)
                .queryParam("path", relPath);

        Response res = processResponse(target.request().post(Entity.entity(input, MediaType.APPLICATION_OCTET_STREAM), Response.class));

        SFHandle shandle = res.readEntity(SFHandle.class);
        LOG.info("/add => {}", shandle);

        return shandle;
    }

    @Override
    public SFHandle get(String rawAddr, String cid, String relPath, Long timeout) throws IOException {

        WebTarget target = client.target(generateURL("/get"))
                .queryParam("addr", rawAddr)
                .queryParam("path", relPath)
                .queryParam("timeout", timeout)
                .queryParam("cid", cid);

        Response res = processResponse(target.request().get(Response.class));

        SFHandle shandle = res.readEntity(SFHandle.class);
        LOG.info("/get => {}", shandle);

        return shandle;
    }

    @Override
    public SFHandle send(String rawAddr, String cid, String rawTarget, Long timeout) throws IOException {

        WebTarget target = client.target(generateURL("/send"))
                .queryParam("addr", rawAddr)
                .queryParam("target", rawTarget)
                .queryParam("cid", cid)
                .queryParam("timeout", timeout);

        Response res = processResponse(target.request().get(Response.class));

        SFHandle shandle = res.readEntity(SFHandle.class);
        LOG.info("/send => {}", shandle);

        return shandle;
    }

    @Override
    public String findAddressRegistation(String rawAddr) throws IOException {

        WebTarget target = client.target(generateURL("/findkey"))
                .queryParam("addr", rawAddr);

        Response res = processResponse(target.request().get(Response.class));
        if (Status.NO_CONTENT.getStatusCode() == res.getStatus()) return null;

        String encKey = res.readEntity(String.class);
        LOG.info("/findkey => {}", encKey);

        return encKey;
    }

    @Override
    public List<SFHandle> findIPFSContent(String rawAddr, Long timeout) throws IOException {

        WebTarget target = client.target(generateURL("/findipfs"))
                .queryParam("addr", rawAddr)
                .queryParam("timeout", timeout);

        Response res = processResponse(target.request().get(Response.class));

        List<SFHandle> result = Arrays.asList(res.readEntity(SFHandle[].class));
        LOG.info("/findipfs => {}", result);

        return result;
    }

    @Override
    public List<String> removeIPFSContent(String rawAddr, List<String> cids) throws IOException {

        WebTarget target = client.target(generateURL("/rmipfs"))
                .queryParam("addr", rawAddr)
                .queryParam("cids", cids.toArray());

        Response res = processResponse(target.request().get(Response.class));

        List<String> result = Arrays.asList(res.readEntity(String[].class));
        LOG.info("/rmipfs => {}", result);

        return result;
    }

    @Override
    public List<SFHandle> findLocalContent(String rawAddr) throws IOException {

        WebTarget target = client.target(generateURL("/findlocal"))
                .queryParam("addr", rawAddr);

        Response res = processResponse(target.request().get(Response.class));

        List<SFHandle> result = Arrays.asList(res.readEntity(SFHandle[].class));
        LOG.info("/findlocal => {}", result);

        return result;
    }

    @Override
    public InputStream getLocalContent(String rawAddr, String path) throws IOException {

        WebTarget target = client.target(generateURL("/getlocal"))
                .queryParam("addr", rawAddr)
                .queryParam("path", path);

        Response res = processResponse(target.request().get(Response.class));

        InputStream content = res.readEntity(InputStream.class);
        LOG.info("/getlocal => {}", content);

        return content;
    }

    @Override
    public boolean removeLocalContent(String rawAddr, String path) throws IOException {

        WebTarget target = client.target(generateURL("/rmlocal"))
                .queryParam("addr", rawAddr)
                .queryParam("path", path);

        Response res = processResponse(target.request().get(Response.class));

        Boolean deleted = res.readEntity(Boolean.class);
        LOG.info("/rmlocal => {}", deleted);

        return deleted;
    }

    private Response processResponse(Response res) throws IOException {
        Status status = Status.fromStatusCode(res.getStatus());
        if (status == Status.INTERNAL_SERVER_ERROR) {

            String stackTrace = res.readEntity(String.class);
            LOG.error(status.getReasonPhrase());
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

            Exception ex;
            try {
                Class<?> clazz = JAXRSClient.class.getClassLoader().loadClass(errorType);
                ex = (Exception) clazz.getConstructor(String.class).newInstance(message);
            } catch (Exception refex) {
                throw new InternalServerErrorException(line);
            }

            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new IllegalStateException(ex);
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
