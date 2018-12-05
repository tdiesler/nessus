## Welcome to Nessus Blockchain + IPFS

The Nessus project is about exploring various Blockchain and related technologies.
In its initial state we looked at the combination of Blockchain and IPFS. 

![preview](ipfs/docs/src/markdown/trail/img/bob-list-02-small.png)

A full walk through of the Blockchain + IPFS demo is here: [QmdJe74VJAfW3JFKBMgfZGPQ1Jnp8YHp6k8uepA3Rztdd8](https://ipfs.io/ipfs/QmdJe74VJAfW3JFKBMgfZGPQ1Jnp8YHp6k8uepA3Rztdd8)

### The Nessus Docker images

In total there are four Docker images that make up the complete system.

1. [nessusio/bitcoind](https://hub.docker.com/r/nessusio/bitcoind)
2. [nessusio/nessus-ipfs](https://hub.docker.com/r/nessusio/ipfs)
3. [nessusio/nessus-jaxrs](https://hub.docker.com/r/nessusio/ipfs-jaxrs)
4. [nessusio/nessus-webui](https://hub.docker.com/r/nessusio/ipfs-webui)

What follows is an installation guide for all four containers. However, if you already have IPFS and Bitcoin Core running locally, you will not need to run these in Docker again.
For a mixed setup with already running IPFS & Bitcoin Core service and newly hosted Docker services go [here](ipfs/docs/src/markdown/setup/Setup-Mixed-Docker.md).

### Quickstart

Here is a quickstart to get the whole system running ...

    export GATEWAYIP=[YOUR_PUBLIC_IP]
    
    docker volume rm -f tnblocks
    docker run --rm -v tnblocks:/var/lib/bitcoind nessusio/bitcoin-tnblocks du -h /var/lib/bitcoind
    docker run --detach --name btcd -p 18333:18333 --expose=18332 -v tnblocks:/var/lib/bitcoind --memory=500m --memory-swap=2g nessusio/bitcoind -testnet=1 -prune=1024
    docker run --detach --name ipfs -p 4001:4001 -p 8080:8080 -e GATEWAYIP=$GATEWAYIP --memory=300m --memory-swap=2g nessusio/ipfs
    docker run --detach --name jaxrs --link btcd:blockchain --link ipfs:ipfs --privileged -v ~/.nessus/plain:/root/.nessus/plain --memory=50m --memory-swap=2g nessusio/ipfs-jaxrs
    docker run --detach --name webui -p 8082:8082 --link btcd:blockchain --link ipfs:ipfs --link jaxrs:jaxrs --memory=50m --memory-swap=2g nessusio/ipfs-webui

It may take a while for the blocks to get loaded from the volume. You would want to see an up to date [blockcount](https://live.blockcypher.com/btc-testnet).

You can watch progress like this

    watch docker exec btcd bitcoin-cli -testnet=1 getblockcount
    docker logs -f btcd

You should now be able to access the WebUI at: [http://127.0.0.1:8082/portal](http://127.0.0.1:8082/portal)

### Updating the installation

Remove all running containers

    docker rm -f `docker ps -aq`

Pull the latest image versions

    docker pull nessusio/bitcoind
    docker pull nessusio/ipfs
    docker pull nessusio/ipfs-jaxrs
    docker pull nessusio/ipfs-webui

Then, start again by running these containers.

### Building this project

You can use the standard maven build process, like this

    mvn clean install

However, running the tests will require to have an IPFS and Bitcoin testnet instances running on your host.
Please follow the instructions for the [mixed setup](ipfs/docs/src/markdown/setup/Setup-Mixed-Docker.md) to get this going.

Enjoy!
