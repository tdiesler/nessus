## Build the JAXRS image

```
export NVERSION=1.0.0-SNAPSHOT

rm -rf docker
mkdir -p docker

tar xzf nessus-ipfs-dist-$NVERSION-deps.tgz -C docker
tar xzf nessus-ipfs-dist-$NVERSION-proj.tgz -C docker

cat << EOF > docker/Dockerfile
FROM nessusio/fedoraj

# Install the binaries
COPY nessus-ipfs-dist-$NVERSION nessus-ipfs-jaxrs

# Make the entrypoint executable
RUN ln -s /nessus-ipfs-jaxrs/bin/run-nessus-jaxrs.sh /usr/local/bin/nessus-jaxrs

ENTRYPOINT ["nessus-jaxrs"]
EOF

docker build -t nessusio/ipfs-jaxrs docker/

docker push nessusio/ipfs-jaxrs
```

### Run the JAXRS image

```
export CNAME=jaxrs

docker rm -f $CNAME
docker run --detach \
    --link ipfs:ipfs \
    --link btcd:blockchain \
    -p 8081:8081 \
    --memory=200m --memory-swap=2g \
    --name $CNAME \
    nessusio/ipfs-jaxrs

# Follow the info log
docker logs -f jaxrs

# Follow the info log on the journal
journalctl CONTAINER_NAME=jaxrs -f

# Follow the debug log
docker exec -it jaxrs tail -f -n 100 debug.log
```

### Run JAXRS Mixed Mode

```
export LOCALIP=192.168.178.20
export NAME=jaxrs

docker rm -f $NAME
docker run --detach \
    -p 8081:8081 \
    --env IPFS_PORT_5001_TCP_ADDR=$LOCALIP \
    --env IPFS_PORT_5001_TCP_PORT=5001 \
    --env IPFS_PORT_8080_TCP_ADDR=$LOCALIP \
    --env IPFS_PORT_8080_TCP_PORT=8080 \
    --env BLOCKCHAIN_JSONRPC_ADDR=$LOCALIP \
    --env BLOCKCHAIN_JSONRPC_PORT=18443 \
    --env BLOCKCHAIN_JSONRPC_USER=rpcusr \
    --env BLOCKCHAIN_JSONRPC_PASS=rpcpass \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    nessusio/ipfs-jaxrs

docker logs -f jaxrs
```    
