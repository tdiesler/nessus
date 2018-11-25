## Build the Bitcoin blockstore image 

```
rm -rf docker
mkdir docker

# https://live.blockcypher.com/btc-testnet

docker exec btcd bitcoin-cli -testnet=1 getblockcount
docker exec btcd bitcoin-cli -testnet=1 stop

docker cp btcd:/var/lib/bitcoind/testnet3 docker/testnet3
rm -f docker/testnet3/*.log docker/testnet3/wallet.dat

cat << EOF > docker/Dockerfile
# Based in Ubuntu 16.04
FROM ubuntu:16.04

# Copy the blockstate
RUN mkdir /var/lib/bitcoind
COPY testnet3 /var/lib/bitcoind/testnet3
EOF

docker build -t nessusio/bitcoin-tnblocks docker/

docker rm btcd
docker volume rm -f tnblocks
docker run --rm -v tnblocks:/var/lib/bitcoind nessusio/bitcoin-tnblocks du -h /var/lib/bitcoind

docker push nessusio/bitcoin-tnblocks
```

## Build the Bitcoin daemon image

```
export NVERSION=0.17.0.1

# Fetch the bootstrap data
wget -O bitcoin-$NVERSION.tgz --no-check-certificate \
    https://bitcoin.org/bin/bitcoin-core-$NVERSION/bitcoin-$NVERSION-x86_64-linux-gnu.tar.gz

rm -rf docker
mkdir docker

# Copy bitcoin binary to the build dir
tar -C docker -xzf bitcoin-$NVERSION.tgz
mv docker/bitcoin-*/ docker/bitcoin-$NVERSION

export RPCUSER=rpcusr
export RPCPASS=rpcpass

cat << EOF > docker/bitcoin-server.conf
rpcuser=$RPCUSER
rpcpassword=$RPCPASS
rpcallowip=172.17.0.1/24
deprecatedrpc=accounts
deprecatedrpc=signrawtransaction
server=1
EOF

cat << EOF > docker/bitcoin-client.conf
rpcuser=$RPCUSER
rpcpassword=$RPCPASS
EOF

cat << EOF > docker/Dockerfile
# Based in Ubuntu 16.04
FROM ubuntu:16.04

# Install the binaries
COPY bitcoin-$NVERSION/bin/bitcoind /usr/bin/
COPY bitcoin-$NVERSION/bin/bitcoin-cli /usr/bin/

# Install the config files
COPY bitcoin-server.conf /etc/bitcoind/bitcoin.conf
COPY bitcoin-client.conf /root/.bitcoin/bitcoin.conf

# Make the data dir
RUN mkdir /var/lib/bitcoind

# Set some default env vars
ENV RPCUSER=$RPCUSER
ENV RPCPASS=$RPCPASS

# Use the daemon as entry point

ENTRYPOINT ["bitcoind", "-datadir=/var/lib/bitcoind", "-conf=/etc/bitcoind/bitcoin.conf"]
EOF

docker build -t nessusio/bitcoind docker/
docker push nessusio/bitcoind

docker tag nessusio/bitcoind nessusio/bitcoind:$NVERSION
docker push nessusio/bitcoind:$NVERSION
```

## Run the Bitcoin testnet

### Populate the blockstore volume

```
docker volume rm tnblocks
docker run -it --rm \
    -v tnblocks:/var/lib/bitcoind \
    nessusio/bitcoin-tnblocks du -h /var/lib/bitcoind

docker volume inspect tnblocks
```

### Run the Bitcoin daemon 

```
export CNAME=btcd

docker rm -f $CNAME
docker run --detach \
    -p 18333:18333 \
    --expose=18332 \
    -v tnblocks:/var/lib/bitcoind \
    --name $CNAME \
    nessusio/bitcoind -testnet=1 -prune=720

watch docker exec btcd bitcoin-cli -testnet=1 getblockcount

docker logs -f btcd
```


