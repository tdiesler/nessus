## Working with the JAXRPC Bridge

Lets assume we don't want to use the WebUI, but instead use JSON-RPC calls only. Lets try ...

First, lets create a new BTC for Bob. 

    docker exec btcd bitcoin-cli -testnet=1 getnewaddress Bob legacy
    mjaktrgwfTteDUtotiAmT1QULd75c1gHmf 	 	
    
When you look at the WebUI again, you should see that Bob's address is now assigned to an account.

Bob then needs some coin, for example from [this](http://bitcoinfaucet.uo1.net/send.php) faucet.

    docker exec btcd bitcoin-cli -testnet=1 getbalance Bob
    
Bob's balance should show up on the explorer like [this](https://live.blockcypher.com/btc-testnet/address/mjaktrgwfTteDUtotiAmT1QULd75c1gHmf)

In case Bob wants to give somebody else some coin ...

    docker exec btcd bitcoin-cli -testnet=1 sendfrom Bob mxitxiMW4Dt1EzRWR569gMzzjmPsGBYBz8 0.1

### Knowing the Bridge IP address

    docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' jaxrs
    172.17.0.4
        
Then run a bash in a docker container like this
    
    docker run -it --rm fedora:29 bash
    
Note, we could also run the various commands below from the host's command line, if the bridge has published (and not just exposed) its main http port.

### Running various JSON-RPC calls on the JAXRPC bridge
 
Lets register Bob's address with the system by making a JSON-RPC call to the bridge.

    curl http://172.17.0.4:8081/nessus/register?addr=mjaktrgwfTteDUtotiAmT1QULd75c1gHmf 	 	

    MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAE9A1T+jvZrehXnx6t1fndvB+QYnitK4d3zTHatJ1Svb4=

This is Bob's public key used for IPFS file encryption.

Lets see, if we can also retrieve it from the blockchain.

    curl http://172.17.0.4:8081/nessus/findkey?addr=mjaktrgwfTteDUtotiAmT1QULd75c1gHmf 	 	

    MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAE9A1T+jvZrehXnx6t1fndvB+QYnitK4d3zTHatJ1Svb4=

Now, lets add this document to the system.

    echo "Hello World" > test.txt
    curl --request POST --data @test.txt http://172.17.0.4:8081/nessus/add?addr=mjaktrgwfTteDUtotiAmT1QULd75c1gHmf&path=test.txt

    {
        "cid": "QmaKutjDrA2nZ9KMaXJRxuy5GftKgKAgA8sG7eeDVewfTZ",
        "owner": "mjaktrgwfTteDUtotiAmT1QULd75c1gHmf",
        "path": "test.txt",
        "txId": "19624717beb6d804b758a2505aedf2f6f42012d699c16354d03528166d84fc28",
        "encrypted": true,
        "available": true,
        "expired": false
    }

Again, this should be reflected in the WebUI.

Connecting to the IPFS gateway direcly, we should be able to see the file content.

    curl http://95.179.128.71:8080/ipfs/QmaKutjDrA2nZ9KMaXJRxuy5GftKgKAgA8sG7eeDVewfTZ

    Nessus-Version: 1.0
    Path: test.txt
    Owner: mjaktrgwfTteDUtotiAmT1QULd75c1gHmf 	 	
    Token: BCeNfw1PzqE3NZvLSkTTi6nEI7qF22NjPDQDH9ZlYA+VxmGqhZnBrfi38w4EOCGkQBueZiN88PdC6ILZP8/f5AvqYaXy
    NESSUS_HEADER_END
    AAAADLPEbHzJJt28GgAYhJS6ZhePaxZ/4alw4xBUXnkMk3IQHtcdmrux9A==

Like above with the public encryption key, we should be able to find this IPFS content id on the blockhain.

    curl http://172.17.0.4:8081/nessus/findipfs?addr=mjaktrgwfTteDUtotiAmT1QULd75c1gHmf 	 	

    {
        "cid": "QmaKutjDrA2nZ9KMaXJRxuy5GftKgKAgA8sG7eeDVewfTZ",
        "owner": "mjaktrgwfTteDUtotiAmT1QULd75c1gHmf",
        "path": "test.txt",
        "txId": "19624717beb6d804b758a2505aedf2f6f42012d699c16354d03528166d84fc28",
        "encrypted": true,
        "available": true,
        "expired": false
    }

The local unencrypted content is also available after the IPFS add.

    curl http://172.17.0.4:8081/nessus/findlocal?addr=mjaktrgwfTteDUtotiAmT1QULd75c1gHmf 	 	

    {
        "cid": null,
        "owner": "mjaktrgwfTteDUtotiAmT1QULd75c1gHmf",
        "path": "test.txt",
        "txId": null,
        "encrypted": false,
        "available": true,
        "expired": false
    }

Lets remove that local file.

    curl http://172.17.0.4:8081/nessus/rmlocal?addr=mjaktrgwfTteDUtotiAmT1QULd75c1gHmf&path=test.txt
    curl http://172.17.0.4:8081/nessus/findlocal?addr=mjaktrgwfTteDUtotiAmT1QULd75c1gHmf 	 	

    []

Lets assume at a later time, we would like to get that file from IPFS

    curl http://172.17.0.4:8081/nessus/get?addr=mjaktrgwfTteDUtotiAmT1QULd75c1gHmf&path=other.txt\&cid=QmaKutjDrA2nZ9KMaXJRxuy5GftKgKAgA8sG7eeDVewfTZ

    {
    "cid": null,
    "owner": "mjaktrgwfTteDUtotiAmT1QULd75c1gHmf",
    "path": "other.txt",
    "txId": null,
    "encrypted": false,
    "available": false,
    "expired": false
    }

Finally, lets get the unencrypted content back

    curl http://172.17.0.4:8081/nessus/getlocal?addr=mjaktrgwfTteDUtotiAmT1QULd75c1gHmf&path=other.txt

    Hello World
    
That's it - Enjoy!

