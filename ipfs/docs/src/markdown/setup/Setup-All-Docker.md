## Setup with Docker services

This type of installation requires a working [Docker](https://www.docker.com/community-edition#/download) environment.

In total there are four Docker images that make up the complete system.

1. [nessusio/bitcoind](https://hub.docker.com/r/nessusio/bitcoind)
2. [nessusio/nessus-ipfs](https://hub.docker.com/r/nessusio/ipfs)
3. [nessusio/nessus-jaxrs](https://hub.docker.com/r/nessusio/ipfs-jaxrs)
4. [nessusio/nessus-webui](https://hub.docker.com/r/nessusio/ipfs-webui)

What follows is an installation guide for all four containers. However, if you already have IPFS and Bitcoin Core running locally, you will not need to run these in Docker again.
For a mixed setup with already running IPFS & Bitcoin Core service and newly hosted Docker services go [here](Setup-Mixed-Docker.md).

For convenience however, lets do the whole setup in Docker first.

### Quickstart

Here is a quickstart to get the whole system running ...

    export GATEWAYIP=192.168.178.20

    docker run --rm -v tnblocks:/var/lib/bitcoind nessusio/bitcoin-tnblocks du -h /var/lib/bitcoind
    docker run --detach --name btcd -p 18333:18333 --expose=18332 -v tnblocks:/var/lib/bitcoind --memory=500m --memory-swap=2g nessusio/bitcoind -testnet=1 -prune=1024
    docker run --detach --name ipfs -p 4001:4001 -p 8080:8080 -e GATEWAYIP=$GATEWAYIP --memory=300m --memory-swap=2g nessusio/ipfs
    docker run --detach --name jaxrs --link btcd:blockchain --link ipfs:ipfs --privileged -v ~/.nessus/plain:/root/.nessus/plain --memory=50m --memory-swap=2g nessusio/ipfs-jaxrs
    docker run --detach --name webui -p 8082:8082 --link btcd:blockchain --link ipfs:ipfs --link jaxrs:jaxrs --memory=50m --memory-swap=2g nessusio/ipfs-webui

It may take a while for the blocks to get loaded from the volume. 
You would want to see an up to date [blockcount](https://live.blockcypher.com/btc-testnet).

You can watch progress like this

    watch docker exec btcd bitcoin-cli -testnet=1 getblockcount
    docker logs -f btcd
    
When everything is running, it should look like this

    docker ps

    CONTAINER ID        IMAGE                 COMMAND                  CREATED             STATUS              PORTS                                                      NAMES
    1f9c469bd20e        nessusio/ipfs-webui   "nessus-webui"           48 seconds ago      Up 46 seconds       0.0.0.0:8082->8082/tcp                                     webui
    c4da82ae45ff        nessusio/ipfs-jaxrs   "nessus-jaxrs"           3 minutes ago       Up 3 minutes        8081/tcp                                                   jaxrs
    a227c571e9c3        nessusio/ipfs         "nessusio-ipfs"          7 minutes ago       Up 7 minutes        0.0.0.0:4001->4001/tcp, 0.0.0.0:8080->8080/tcp, 5001/tcp   ipfs
    a6a327378fde        nessusio/bitcoind     "bitcoind -datadir..."   20 minutes ago      Up 20 minutes       18332/tcp, 0.0.0.0:18333->18333/tcp                        btcd

You should now be able to access the WebUI at: [http://127.0.0.1:8082/portal](http://127.0.0.1:8082/portal)

Bob then needs some coin, for example from [this](http://bitcoinfaucet.uo1.net/send.php) faucet.

### Connecting the IPFS instance to a swarm

When two parties exchange IPFS documents, it is useful for them to be connected to each other.
Otherwise IPFS operations may take considerably longer to find the wanted documents. 

You can get the network ID like this ...

    echo "ipfs swarm connect /ip4/$GATEWAYIP/tcp/4001/ipfs/`docker exec ipfs ipfs config Identity.PeerID`"

and then on some other IPFS instance connect to the Nessus IPFS daemon like this ...

    ipfs swarm connect /ip4/192.168.178.20/tcp/4001/ipfs/QmXzS59vnQhKTunD6DGqnj9GgkaYFJtqFzngdhw3f8AmHC

### Examine the various container logs 

You can always get the logs for a running container like this ...

    docker logs ipfs

    initializing IPFS node at /root/.ipfs
    generating 2048-bit RSA keypair...done
    peer identity: QmXzS59vnQhKTunD6DGqnj9GgkaYFJtqFzngdhw3f8AmHC
    
    Initializing daemon...
    go-ipfs version: 0.4.18-
    Repo version: 7
    System version: 386/linux
    Golang version: go1.11.1
    Swarm listening on /ip4/127.0.0.1/tcp/4001
    Swarm listening on /ip4/172.17.0.3/tcp/4001
    Swarm listening on /p2p-circuit
    Swarm announcing /ip4/127.0.0.1/tcp/4001
    Swarm announcing /ip4/172.17.0.3/tcp/4001
    API server listening on /ip4/0.0.0.0/tcp/5001
    Gateway (readonly) server listening on /ip4/0.0.0.0/tcp/8080

On bootstrap the bridge reports some connection properties.

    docker logs jaxrs

    IPFS Address: /ip4/172.17.0.3/tcp/5001
    IPFS Version: 0.4.18
    BitcoinBlockchain: http://rpcusr:*******@172.17.0.2:18332
    BitcoinNetwork Version: 170001
    DefaultContentManager[timeout=6000, attempts=100, threads=12]
    Nessus JAXRS: http://0.0.0.0:8081/nessus

The WebUI also reports some connection properties.

    docker logs webui

    IPFS Gateway: http://192.168.178.20:8080/ipfs
    BitcoinBlockchain: http://rpcusr:*******@172.17.0.2:18332
    BitcoinNetwork Version: 170001
    Nessus JAXRS: http://172.17.0.4:8081/nessus
    Nessus WebUI: http://0.0.0.0:8082/portal

That's it - Enjoy!
