## Build the JAXRS image

```
export NVERSION=1.0.0.Beta3-SNAPSHOT

rm -rf docker
mkdir -p docker

tar xzf nessus-ipfs-dist-$NVERSION-deps.tgz -C docker
tar xzf nessus-ipfs-dist-$NVERSION-proj.tgz -C docker

cat << EOF > docker/Dockerfile
FROM nessusio/fedoraj:29

# Install the binaries
COPY nessus-ipfs-dist-$NVERSION nessus-ipfs-jaxrs

# Make the entrypoint executable
RUN ln -s /nessus-ipfs-jaxrs/bin/run-nessus-jaxrs.sh /usr/local/bin/nessus-jaxrs

# Expose the JAXRS port
EXPOSE 8081

ENTRYPOINT ["nessus-jaxrs"]
EOF

docker build -t nessusio/ipfs-jaxrs docker/
docker push nessusio/ipfs-jaxrs

docker tag nessusio/ipfs-jaxrs nessusio/ipfs-jaxrs:$NVERSION
docker push nessusio/ipfs-jaxrs:$NVERSION
```

### Run the JAXRS image

```
export CNAME=jaxrs

docker rm -f $CNAME
docker run --detach \
    --link ipfs:ipfs \
    --link btcd:blockchain \
    --memory=100m --memory-swap=2g \
    --name $CNAME \
    nessusio/ipfs-jaxrs

# Follow the info log
docker logs -f jaxrs

# Follow the info log on the journal
journalctl CONTAINER_NAME=jaxrs -f

# Follow the debug log
docker exec -it jaxrs tail -f -n 100 debug.log
```

### Run the JAXRS in mixed mode

This assumes you have the Blockchain and IPFS instances already running on your host

```
export CNAME=jaxrs
export LOCALIP=192.168.178.20

docker rm -f $CNAME
docker run --detach \
    --env IPFS_JSONRPC_ADDR=$LOCALIP \
    --env IPFS_JSONRPC_PORT=5001 \
    --env BLOCKCHAIN_JSONRPC_ADDR=$LOCALIP \
    --env BLOCKCHAIN_JSONRPC_PORT=18332 \
    --env BLOCKCHAIN_JSONRPC_USER=rpcusr \
    --env BLOCKCHAIN_JSONRPC_PASS=rpcpass \
    --memory=100m --memory-swap=2g \
    --name $CNAME \
    nessusio/ipfs-jaxrs
    
docker logs jaxrs
```
