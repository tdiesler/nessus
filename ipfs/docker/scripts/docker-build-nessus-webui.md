## Build the Nessus WebUI image

```
export NVERSION=1.0.0.Beta2

rm -rf docker
mkdir -p docker

tar xzf nessus-ipfs-dist-$NVERSION-deps.tgz -C docker
tar xzf nessus-ipfs-dist-$NVERSION-proj.tgz -C docker

cat << EOF > docker/Dockerfile
FROM nessusio/fedoraj:29

# Install the binaries
COPY nessus-ipfs-dist-$NVERSION nessus-ipfs-webui

# Make the entrypoint executable
RUN ln -s /nessus-ipfs-webui/bin/run-nessus-webui.sh /usr/local/bin/nessus-webui

ENTRYPOINT ["nessus-webui"]
EOF

docker build -t nessusio/ipfs-webui docker/
docker push nessusio/ipfs-webui

docker tag nessusio/ipfs-webui nessusio/ipfs-webui:$NVERSION
docker push nessusio/ipfs-webui:$NVERSION
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
    --memory=100m --memory-swap=2g \
    --name $CNAME \
    nessusio/ipfs-webui

docker logs -f webui

docker exec -it webui tail -f -n 100 debug.log
```

### Run the WebUI in mixed mode

This assumes you have the Blockchain and IPFS instances already running on your host

```
export CNAME=webui
export LOCALIP=192.168.178.20
export LABEL=Mary

docker rm -f $CNAME
docker run --detach \
    -p 8082:8082 \
    --link jaxrs:jaxrs \
    --env IPFS_GATEWAY_ADDR=$LOCALIP \
    --env IPFS_GATEWAY_PORT=8080 \
    --env BLOCKCHAIN_JSONRPC_ADDR=$LOCALIP \
    --env BLOCKCHAIN_JSONRPC_PORT=18332 \
    --env BLOCKCHAIN_JSONRPC_USER=rpcusr \
    --env BLOCKCHAIN_JSONRPC_PASS=rpcpass \
    --env NESSUS_WEBUI_LABEL=$LABEL \
    --memory=100m --memory-swap=2g \
    --name $CNAME \
    nessusio/ipfs-webui

docker logs -f webui
```