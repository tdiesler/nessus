## Build the Nessus IPFS image

### Build the Golang image 

```
rm -rf docker
mkdir -p docker

cat << EOF > docker/Dockerfile
FROM fedora:28

RUN dnf -y install golang

CMD ["version"]

ENTRYPOINT ["go"]
EOF

docker rmi -f nessusio/golang
docker build -t nessusio/golang docker/

docker run --rm nessusio/golang
docker push nessusio/golang

docker tag nessusio/golang nessusio/golang:1.10.3
docker push nessusio/golang:1.10.3
```

### Build the IPFS image

This is largly inspired by [Run IPFS latest on a VPS](https://ipfs.io/blog/22-run-ipfs-on-a-vps/).

```
export NVERSION=0.4.18
export PLATFORM=linux-386
wget --no-check-certificate https://dist.ipfs.io/go-ipfs/v$NVERSION/go-ipfs_v"$NVERSION"_"$PLATFORM".tar.gz

rm -rf docker
mkdir -p docker

tar xzf go-ipfs_*.tar.gz -C docker/

export RUNNAME=run-ipfs.sh
cat << EOF > docker/$RUNNAME
#!/bin/bash

IPFS_CONFIG="/root/.ipfs/config"
if [ ! -f IPFS_CONFIG ]; then

    ipfs init
    
    ipfs config Addresses.API "/ip4/0.0.0.0/tcp/5001"
    ipfs config Addresses.Gateway "/ip4/0.0.0.0/tcp/8080"
    ipfs config --json Addresses.Swarm '["/ip4/0.0.0.0/tcp/4001"]'
fi

# Start the IPFS daemon
ipfs daemon
EOF

cat << EOF > docker/Dockerfile
FROM nessusio/golang

COPY go-ipfs/ipfs /usr/local/bin/

COPY $RUNNAME /root/$RUNNAME
RUN chmod +x /root/$RUNNAME

# Make the entrypoint executable
RUN ln -s /root/$RUNNAME /usr/local/bin/run-ipfs

# Expose the API port
EXPOSE 5001

ENTRYPOINT ["run-ipfs"]
EOF

docker rmi -f nessusio/ipfs
docker build -t nessusio/ipfs docker/

docker tag nessusio/ipfs nessusio/ipfs:$NVERSION

docker push nessusio/ipfs:$NVERSION
docker push nessusio/ipfs

```

### Run the IPFS image 

```
export CNAME=ipfs
export GATEWAYIP=192.168.178.20

docker rm -f $CNAME
docker run --detach \
    -p 4001:4001 \
    -p $GATEWAYIP:8080:8080 \
    --memory=300m --memory-swap=2g \
    --name $CNAME \
    nessusio/ipfs

docker logs ipfs

http://127.0.0.1:5001/webui
```
