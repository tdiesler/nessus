## Setup Guide for Bob

The steps to get the demo running on your box are trivial. It assumes that you already have [Docker](https://www.docker.com/community-edition) running.
If not, there is guide on how to setup a VPS that has Docker support [here](../setup/Setup-VPS-Docker.md). These steps would however equally work on your Mac or Windows box.

### Quickstart

In case you know what you're doing already. Here is the quickstart to get the whole system running in no time ...

    export GATEWAYIP=192.168.178.20

    docker run --rm -v tnblocks:/var/lib/bitcoind nessusio/bitcoin-tnblocks du -h /var/lib/bitcoind
    docker run --detach --name btcd -p 18333:18333 --expose=18332 -v tnblocks:/var/lib/bitcoind --memory=300m --memory-swap=2g nessusio/bitcoind -testnet=1 -prune=720
    docker run --detach --name ipfs -p 4001:4001 -p 8080:8080 -e GATEWAYIP=$GATEWAYIP --memory=300m --memory-swap=2g nessusio/ipfs
    docker run --detach --name jaxrs --link btcd:blockchain --link ipfs:ipfs --memory=100m --memory-swap=2g nessusio/ipfs-jaxrs
    docker run --detach --name webui -p 8082:8082 --link btcd:blockchain --link ipfs:ipfs --link jaxrs:jaxrs -e NESSUS_WEBUI_LABEL=Bob --memory=100m --memory-swap=2g nessusio/ipfs-webui

It'll take a little while for the network to sync. You can watch progress like this ...

    docker logs -f btcd
    docker exec btcd bitcoin-cli -testnet=1 getblockcount

Bob then needs some coin, for example from [this](http://bitcoinfaucet.uo1.net/send.php) faucet.

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

### Setup for Mary (optional)

For the more advanced __send__ functionality you may want to also follow the [setup for Mary](Setup-Guide-Mary.md).

That's it - Enjoy!
