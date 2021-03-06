## Build the Nessus WebUI image

```
export NVERSION=1.0.0.Beta3

rm -rf docker
mkdir -p docker

tar xzf nessus-ipfs-dist-$NVERSION-deps.tgz -C docker
tar xzf nessus-ipfs-dist-$NVERSION-proj.tgz -C docker

cat << EOF > docker/Dockerfile
FROM nessusio/centosj:7

# Install the binaries
COPY nessus-ipfs-dist-$NVERSION nessus-ipfs-webui

# Make the entrypoint executable
RUN ln -s /nessus-ipfs-webui/bin/run-webui.sh /usr/local/bin/run-webui

ENTRYPOINT ["run-webui"]
EOF

docker build -t nessusio/ipfs-webui docker/
docker tag nessusio/ipfs-webui nessusio/ipfs-webui:$NVERSION

docker push nessusio/ipfs-webui
docker push nessusio/ipfs-webui:$NVERSION
```

### Run the WebUI image

```
docker rm -f webui
docker run --detach \
    -p 8082:8082 \
    --link ipfs:ipfs \
    --link jaxrs:jaxrs \
    --link btcd:blockchain \
    --memory=100m --memory-swap=2g \
    --name webui \
    nessusio/ipfs-webui

docker logs -f webui

docker exec -it webui tail -f -n 100 debug.log
```

### Run the WebUI in mixed mode

This assumes you have the Blockchain and IPFS instances already running on your host

```
# Testnet: 18332
# Regtest: 18443

export LOCALIP=192.168.178.20
export RPCPORT=18332
export LABEL=Mary

docker rm -f webui
docker run --detach \
    -p 8082:8082 \
    --link jaxrs:jaxrs \
    --env IPFS_JSONRPC_ADDR=$LOCALIP \
    --env IPFS_JSONRPC_PORT=5001 \
    --env IPFS_GATEWAY_ADDR=$LOCALIP \
    --env IPFS_GATEWAY_PORT=8080 \
    --env BLOCKCHAIN_JSONRPC_ADDR=$LOCALIP \
    --env BLOCKCHAIN_JSONRPC_PORT=$RPCPORT \
    --env BLOCKCHAIN_JSONRPC_USER=rpcusr \
    --env BLOCKCHAIN_JSONRPC_PASS=rpcpass \
    --env NESSUS_WEBUI_LABEL=$LABEL \
    --name webui \
    nessusio/ipfs-webui

docker logs -f webui
```
