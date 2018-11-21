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
import java.security.GeneralSecurityException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/")
public interface JAXRSEndpoint {

    @GET
    @Path("/register")
    @Produces(MediaType.TEXT_PLAIN)
    String registerAddress(@QueryParam("addr") String rawAddr) throws GeneralSecurityException, IOException;

    @GET
    @Path("/unregaddr")
    @Produces(MediaType.TEXT_PLAIN)
    String unregisterAddress(@QueryParam("addr") String rawAddr) throws IOException;
    
    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    SFHandle add(@QueryParam("addr") String rawAddr, @QueryParam("path") String path, InputStream input) throws IOException, GeneralSecurityException;

    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_JSON)
    SFHandle get(@QueryParam("addr") String rawAddr, @QueryParam("cid") String cid, @QueryParam("path") String path, @QueryParam("timeout") Long timeout) throws IOException, GeneralSecurityException;

    @GET
    @Path("/send")
    @Produces(MediaType.APPLICATION_JSON)
    SFHandle send(@QueryParam("addr") String rawAddr, @QueryParam("cid") String cid, @QueryParam("target") String rawTarget, @QueryParam("timeout") Long timeout) throws IOException, GeneralSecurityException;

    @GET
    @Path("/findkey")
    @Produces(MediaType.APPLICATION_JSON)
    String findAddressRegistation(@QueryParam("addr") String rawAddr) throws IOException;

    @GET
    @Path("/findipfs")
    @Produces(MediaType.APPLICATION_JSON)
    List<SFHandle> findIPFSContent(@QueryParam("addr") String rawAddr, @QueryParam("timeout") Long timeout) throws IOException;

    @GET
    @Path("/unregipfs")
    @Produces(MediaType.APPLICATION_JSON)
    List<SFHandle> unregisterIPFSContent(@QueryParam("addr") String rawAddr, @QueryParam("cids") List<String> cids) throws IOException;
    
    @GET
    @Path("/findlocal")
    @Produces(MediaType.APPLICATION_JSON)
    List<SFHandle> findLocalContent(@QueryParam("addr") String rawAddr) throws IOException;

    @GET
    @Path("/getlocal")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    InputStream getLocalContent(@QueryParam("addr") String rawAddr, @QueryParam("path") String path) throws IOException;

    @GET
    @Path("/dellocal")
    @Produces(MediaType.TEXT_PLAIN)
    boolean deleteLocalContent(@QueryParam("addr") String rawAddr, @QueryParam("path") String path) throws IOException;
}
