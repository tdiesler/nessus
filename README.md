## Welcome to Nessus Blockchain + IPFS

The Nessus project is about exploring various Blockchain and related technologies.
In its initial state we looked at the combination of Blockchain and IPFS. 

![preview](ipfs/docs/src/markdown/trail/img/bob-list-02-small.png)

A full walk through of the Blockchain + IPFS demo is here: [Qmac3JJcAVJFaKsUcgQFGy3mxXSt2mcZdLhMh3eEpvLJB5](https://ipfs.io/ipfs/Qmac3JJcAVJFaKsUcgQFGy3mxXSt2mcZdLhMh3eEpvLJB5)

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
    
    docker run --rm -v blockstore:/var/lib/bitcoind nessusio/bitcoin-testnet-blockstore du -h /var/lib/bitcoind
    docker run --detach --name ipfs -p 4001:4001 -p 8080:8080 --env GATEWAYIP=$GATEWAYIP --memory=300m --memory-swap=2g nessusio/ipfs
    docker run --detach --name btcd -p 18333:18333 --expose=18332 -v blockstore:/var/lib/bitcoind --memory=400m --memory-swap=2g nessusio/bitcoind -testnet=1 -prune=1024
    docker run --detach --name jaxrs --link btcd:blockchain --link ipfs:ipfs --privileged -v ~/.nessus/plain:/root/.nessus/plain --memory=100m --memory-swap=2g nessusio/ipfs-jaxrs
    docker run --detach --name webui -p 8082:8082 --link btcd:blockchain --link ipfs:ipfs --link jaxrs:jaxrs --env NESSUS_WEBUI_LABEL=Bob --memory=100m --memory-swap=2g nessusio/ipfs-webui

When everything is running, it should look like this ...

	CONTAINER ID        IMAGE                 COMMAND                  CREATED             STATUS              PORTS                                                      NAMES
	7487e74cdc23        nessusio/ipfs-webui   "run-webui"              5 seconds ago       Up 4 seconds        0.0.0.0:8082->8082/tcp                                     webui
	bffb2ec324f5        nessusio/ipfs-jaxrs   "run-jaxrs"              5 seconds ago       Up 5 seconds        8081/tcp                                                   jaxrs
	c927c4a69880        nessusio/ipfs         "nessusio-ipfs"          6 seconds ago       Up 5 seconds        0.0.0.0:4001->4001/tcp, 0.0.0.0:8080->8080/tcp, 5001/tcp   ipfs
	30a2dd595ccd        nessusio/bitcoind     "bitcoind -datadir..."   7 seconds ago       Up 6 seconds        18332/tcp, 0.0.0.0:18333->18333/tcp                        btcd

It may take a while for the blocks to get loaded from the volume. When done, you would want to see an up to date [blockcount](https://live.blockcypher.com/btc-testnet).

You can watch progress like this

    watch docker exec btcd bitcoin-cli -testnet=1 getblockcount
    docker logs -f btcd

The `jaxrs` image provides JaxRs API, whih is documented [here](https://github.com/jboss-fuse/nessus/blob/master/ipfs/jaxrs/src/main/java/io/nessus/ipfs/jaxrs/JAXRSEndpoint.java#L37). 
You should see initial logging output like this

	$ docker logs jaxrs
	Nessus Version: 1.0.0.Beta3
	BitcoinBlockchain: http://rpcusr:*******@172.17.0.2:18332
	BitcoinNetwork Version: 170001
	IPFS PeerId: QmRdphVM5wgF7NCw4udyfBn1Fd1YsSCGmEguaViTeB5n5Z
	IPFS Address: /ip4/172.17.0.3/tcp/5001
	IPFS Version: 0.4.18
	DefaultContentManager[dataDir=/root/.nessus, timeout=6000, attempts=100, threads=12, overwrite=false]
	Nessus JAXRS: http://0.0.0.0:8081/nessus

The `webui` image provides a simple web based user interface, which interacts with the JaxRs API. 
You should see initial logging output like this

	$ docker logs webui
	Nessus Version: 1.0.0.Beta3
	BitcoinBlockchain: http://rpcusr:*******@172.17.0.2:18332
	BitcoinNetwork Version: 170001
	IPFS PeerId: QmYy99L7A5CBCogoSJ4my7W2uzbY3Xk7UzHSADMpvzXGWy
	IPFS Gateway: http://95.179.182.138:8080/ipfs
	IPFS Address: /ip4/172.17.0.3/tcp/5001
	IPFS Version: 0.4.18
	Nessus JAXRS: http://172.17.0.4:8081/nessus
	Nessus WebUI: http://0.0.0.0:8082/portal

You should now be able to access the WebUI at: [http://127.0.0.1:8082/portal](http://127.0.0.1:8082/portal)

### Connecting IPFS nodes to a swarm

If you run multiple IPFS nodes, it is a good idea to connect them in a swarm. 
You can get the ipfs connect command like this ...   
	
	$ echo "ipfs swarm connect /ip4/$GATEWAYIP/tcp/4001/ipfs/`docker exec ipfs ipfs config Identity.PeerID`"
	ipfs swarm connect /ip4/95.179.182.138/tcp/4001/ipfs/QmQE2kRHP9FDPQKorAkmZvxS8gvip5oQvPkEsmdRKPdAN4
 
On the other node you can connect like this ...
 
 	$ docker exec ipfs ipfs swarm connect /ip4/95.179.182.138/tcp/4001/ipfs/QmQE2kRHP9FDPQKorAkmZvxS8gvip5oQvPkEsmdRKPdAN4
 	connect QmYy99L7A5CBCogoSJ4my7W2uzbY3Xk7UzHSADMpvzXGWy success
 
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
