## Build the Nessus WebUI image

```
rm -rf docker
mkdir -p docker

export NVERSION=1.0.0-SNAPSHOT

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

Run the Nessus WebUI

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
