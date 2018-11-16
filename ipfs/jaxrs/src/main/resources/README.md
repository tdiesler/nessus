Nessus JAXRS
============

REST API
--------

    @GET
    @Path("/register")
    @Produces(MediaType.TEXT_PLAIN)
    String register(@QueryParam("addr") String rawAddr)

    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    SFHandle add(@QueryParam("addr") String rawAddr, @QueryParam("path") String path, InputStream input)

    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_JSON)
    SFHandle get(@QueryParam("addr") String rawAddr, @QueryParam("cid") String cid, @QueryParam("path") String path, @QueryParam("timeout") Long timeout)

    @GET
    @Path("/send")
    @Produces(MediaType.APPLICATION_JSON)
    SFHandle send(@QueryParam("addr") String rawAddr, @QueryParam("cid") String cid, @QueryParam("target") String rawTarget, @QueryParam("timeout") Long timeout)

    @GET
    @Path("/findkey")
    @Produces(MediaType.APPLICATION_JSON)
    String findRegistation(@QueryParam("addr") String rawAddr)

    @GET
    @Path("/findipfs")
    @Produces(MediaType.APPLICATION_JSON)
    List<SFHandle> findIPFSContent(@QueryParam("addr") String rawAddr, @QueryParam("timeout") Long timeout)

    @GET
    @Path("/findlocal")
    @Produces(MediaType.APPLICATION_JSON)
    List<SFHandle> findLocalContent(@QueryParam("addr") String rawAddr)

    @GET
    @Path("/getlocal")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    InputStream getLocalContent(@QueryParam("addr") String rawAddr, @QueryParam("path") String path)

    @GET
    @Path("/dellocal")
    @Produces(MediaType.TEXT_PLAIN)
    boolean deleteLocalContent(@QueryParam("addr") String rawAddr, @QueryParam("path") String path)
