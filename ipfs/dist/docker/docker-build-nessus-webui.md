## Build the Nessus WebUI image

```
export NVERSION=1.0.0-SNAPSHOT

rm -rf docker
mkdir -p docker

tar xzf nessus-ipfs-dist-$NVERSION-deps.tgz -C docker
tar xzf nessus-ipfs-dist-$NVERSION-proj.tgz -C docker

cat << EOF > docker/Dockerfile
FROM nessusio/fedoraj

# Install the binaries
COPY nessus-ipfs-dist-$NVERSION nessus-ipfs-webui

# Make the entrypoint executable
RUN ln -s /nessus-ipfs-webui/bin/run-nessus-webui.sh /usr/local/bin/nessus-webui

ENTRYPOINT ["nessus-webui"]
EOF

docker build -t nessusio/ipfs-webui docker/

docker push nessusio/ipfs-webui
```

### Run the WebUI image

```
export CNAME=webui

docker rm -f $CNAME
docker run --detach \
    -p 8082:8082 \
    --link ipfs:ipfs \
    --link jaxrs:jaxrs \
    --link btcd:blockchain \
    --memory=200m --memory-swap=2g \
    --name $CNAME \
    nessusio/ipfs-webui

# Follow the info log
docker logs -f webui

# Follow the info log on the journal
journalctl CONTAINER_NAME=webui -f

# Follow the debug log
docker exec -it webui tail -f -n 100 debug.log
```

### Run the WebUI Mixed Mode

```
export NAME=webui
export LABEL=Bob

docker rm -f $NAME
docker run --detach \
    -p 8082:8082 \
    --link jaxrs:jaxrs \
    --env IPFS_PORT_5001_TCP_ADDR=$LOCALIP \
    --env IPFS_PORT_5001_TCP_PORT=5001 \
    --env IPFS_PORT_8080_TCP_ADDR=$LOCALIP \
    --env IPFS_PORT_8080_TCP_PORT=8080 \
    --env BLOCKCHAIN_JSONRPC_ADDR=$LOCALIP \
    --env BLOCKCHAIN_JSONRPC_PORT=18443 \
    --env BLOCKCHAIN_JSONRPC_USER=rpcusr \
    --env BLOCKCHAIN_JSONRPC_PASS=rpcpass \
    --env NESSUS_WEBUI_LABEL=$LABEL \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    nessusio/ipfs-webui
    
docker logs -f webui
```
