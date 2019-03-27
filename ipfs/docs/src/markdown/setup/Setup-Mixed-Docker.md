## Local setup with mixed services

This type of installation requires a working [Docker](https://www.docker.com/community-edition#/download) environment.

In total there are four Docker images to make up the complete system.

1. [nessusio/bitcoind](https://hub.docker.com/r/nessusio/bitcoind)
2. [nessusio/nessus-ipfs](https://hub.docker.com/r/nessusio/ipfs)
3. [nessusio/nessus-jaxrs](https://hub.docker.com/r/nessusio/ipfs-jaxrs)
4. [nessusio/nessus-webui](https://hub.docker.com/r/nessusio/ipfs-webui)

What follows is an installation guide for the last two containers.
It is assumed that you already have a local IPFS and some blockchain wallet running.

### Running the JAXRS image

This is the JSON-RPC bridge, which contains the Nessus IPFS application logic that connects the Blockchain network with IPFS network.

#### Bind the Bitcoin wallet to an external IP

For this to work, we use a Bitcoin wallet which needs to bind to an external IP

    server=1
    rpcuser=rpcusr
    rpcpassword=rpcpass
    rpcbind=192.168.178.20
    rpcallowip=192.168.178.20
    deprecatedrpc=accounts
    deprecatedrpc=signrawtransaction
    txindex=1
    testnet=1

Verify that this works

    curl --data-binary '{"method": "getblockcount"}' http://rpcusr:rpcpass@192.168.178.20:18332

Then, verify that this also works from within docker

    docker run -it --rm --entrypoint=bash centos
    curl --data-binary '{"method": "getblockcount"}' http://rpcusr:rpcpass@192.168.178.20:18332

#### Bind the IPFS daemon to an external IP

For this to work, your IPFS daemon needs to bind to an external IP

    ipfs config Addresses.API "/ip4/0.0.0.0/tcp/5001"
    ipfs config Addresses.Gateway "/ip4/0.0.0.0/tcp/8080"
    ipfs daemon &
    ...
    API server listening on /ip4/0.0.0.0/tcp/5001
    Gateway (readonly) server listening on /ip4/0.0.0.0/tcp/8080
    Daemon is ready

Verify that this works

    ipfs --api=/ip4/192.168.178.20/tcp/5001 version

#### Run the JAXRS image

To start the Nessus bridge in Docker, you can run ...

	# Testnet: 18332
	# Regtest: 18443
	
	export LOCALIP=192.168.178.20
	export RPCPORT=18443

    docker run --detach \
        -p 8081:8081 \
        --env IPFS_JSONRPC_ADDR=$LOCALIP \
        --env IPFS_JSONRPC_PORT=5001 \
        --env BLOCKCHAIN_JSONRPC_ADDR=$LOCALIP \
        --env BLOCKCHAIN_JSONRPC_PORT=$RPCPORT \
        --env BLOCKCHAIN_JSONRPC_USER=rpcusr \
        --env BLOCKCHAIN_JSONRPC_PASS=rpcpass \
        --name jaxrs \
        nessusio/ipfs-jaxrs

On bootstrap the bridge reports some connection properties.

    docker logs jaxrs

    IPFS Address: /ip4/192.168.178.20/tcp/5001
    IPFS Version: 0.4.18
    BitcoinBlockchain: http://rpcusr:*******@192.168.178.20:18443
    BitcoinNetwork Version: 170001
    DefaultContentManager[timeout=6000, attempts=100, threads=12]
    Nessus JAXRS: http://0.0.0.0:8081/nessus

#### Knowing the Bridge IP address

	docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' jaxrs
	172.17.0.4

### Running the WebUI image

In this setup the Nessus UI is optional as well. Still, lets try to connect it to the JSON-RPC bridge and the Blockchain wallet  ...

	# Testnet: 18332
	# Regtest: 18443
	
	export LOCALIP=192.168.178.20
	export RPCPORT=18443
    	export LABEL=Mary

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

The WebUI also reports some connection properties.

    docker logs webui

    IPFS Gateway: http://192.168.178.20:8080/ipfs
    BitcoinBlockchain: http://rpcusr:*******@192.168.178.20:18443
    BitcoinNetwork Version: 170001
    Nessus JAXRS: http://172.17.0.2:8081/nessus
    Nessus WebUI: http://0.0.0.0:8082/portal

Now that everything is running, it should look like this

    docker ps

    CONTAINER ID        IMAGE                 COMMAND             CREATED             STATUS              PORTS                    NAMES
    83d7e9434c5a        nessusio/ipfs-webui   "nessus-webui"      9 seconds ago       Up 8 seconds        0.0.0.0:8082->8082/tcp   webui
    7abd814c70d3        nessusio/ipfs-jaxrs   "nessus-jaxrs"      18 seconds ago      Up 17 seconds       8081/tcp                 jaxrs

You should now be able to access the WebUI at: [http://127.0.0.1:8082/portal](http://127.0.0.1:8082/portal)

That's it - Enjoy!

