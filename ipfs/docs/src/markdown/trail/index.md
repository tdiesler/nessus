## Blockchain + IPFS Demo

Welcome, I'm glad you made it to this page.

In this demo we connect to two low cost [VPS](https://www.vultr.com). Each hosting a [Docker](https://www.docker.com/community-edition) environment with these service.

* IPFS
* Bitcoin Testnet
* Nessus JAXRS 
* Nessus WebUI 

Networking between these containers is isolated by Docker. The demo records OP_RETURN data on the Bitcoin network to provide proof of ownership and encryption capabilies of content stored on IPFS. 

Without further ado, lets dive right into what we have here ...

### Bob visits Nessus

Our first actor is Bob. He has heard about what [Nessus](https://github.com/jboss-fuse/nessus) is doing with IPFS and would like to give Mary, who we will meet later, an encrypted copy of his medical records. For ensurance reasons, he needs proof that Mary has been given access to these records by a certain date. Needless to say, he does not want anybody else, but Mary, to be able to access these records.

Bob visits Nessus here: [http://95.179.128.71:8082/portal](http://95.179.128.71:8082/portal)

![bob-home-01](img/bob-home-01.png)

#### New Address

Here we can create a new address for Bob.

![bob-home-02](img/bob-home-02.png)

#### Receiving Addresses

Is the list of addresses that Bob's wallet knows about. Initially, we see the single default address that we just created. 
This address has no funds yet.

Bob's balance should show up on the explorer like [this](https://live.blockcypher.com/btc-testnet/address/mjaktrgwfTteDUtotiAmT1QULd75c1gHmf)

#### Import Key

Here we can import existing addresses from people we want to send files to.

### Give Bob some coin

When the demo records stuff on the blockchain it will have to pay network fees and a small amount for every data record.
Hence, giving Bob some coin from a [testnet faucet](http://bitcoinfaucet.uo1.net/send.php) is plenty for this demo.

![bob-home-03](img/bob-home-03.png)

### Register Bob's public key

All file content stored on IPFS is encrypted with a key derived from the owner's private key. This encryption key is now registered on the blockchain so that others can find it. 

We register the Nessus public encryption key by clicking on __register__.

![bob-home-04](img/bob-home-04.png)

We can head over to the block exporer and look at Tx: [76a6341ea40d101912e4ab59f48ed356cb995eac23164482d84f0b2b7441d922](https://live.blockcypher.com/btc-testnet/tx/76a6341ea40d101912e4ab59f48ed356cb995eac23164482d84f0b2b7441d922/)

### Bob adds his medical records

By clicking on one of Bob's addresses, we come to a page that lists the file content stored on IPFS. At this stage, the list is empty. Please note, that this does actually not query the IPFS network. Instead it queries the blockchain for references to IPFS files associated with this address.

![bob-list-01](img/bob-list-01.png)

We procede in the obvious way ...

![bob-add-01](img/bob-add-01.png)

#### Add by Content

Allows us to add some content directly from this page, which we will do by clicking the first __Submit__ button.

#### Add by URL

Allows us to add some content from an URL.

### Bob inspects his IPFS content

We are back on the page that lists Bob's IPFS content and voila, we see the IPFS hash that has just been associated with Bob's address.

![bob-list-02](img/bob-list-02.png)

We can now click on the IPFS content ID to see the encrypted content as it is known to IPFS.

    Nessus-Version: 1.0
    Path: Bob/file01.txt
    Owner: mjaktrgwfTteDUtotiAmT1QULd75c1gHmf
    Token: BA2o3ekuTMinR6NXBDQyjLoalY+YLuems5r+XjuMshntLxUFhX01ANLWtGCDIVJFHoACLAbp/I2rzavmLiAo++TLbpuW
    NESSUS_HEADER_END
    AAAADEpH9tHUBp2rH1R53VjbnDRS4SxENDnWpYcIK4VAwK38NdjZ8bWgkwtFJWbj/RZgY31lCIVQYu6Dq8UhnIVU

### Bob decrypts his medical record

On the IPFS file list, we can also __get__ encrypted content from IPFS and save a decrypted copy locally

The Nessus app can then __show__ us the decrypted content.

![bob-show-01](img/bob-show-01.png)

### Bob imports Mary's address

Bob imports Mary's address like this

![bob-home-05](img/bob-home-05.png)

Because Bob does not own Mary's private key, this is a "watch only" address. It allows Bob's wallet to see Mary's transactions - specifically her public encryption key.

![bob-home-06](img/bob-home-06.png)

### Mary visits Nessus

Mary also has access to Nessus. Like Bob, she registers her public key.
Please note, that if Bob is not using a full blockchain node which would allow reindexing, this must happen after Bob has imported Mary's address.

While Bob has setup his instance on a VPS. Mary has done so on her MacBook. 
She can visit Nessus here: [http://127.0.0.1:8082/portal](http://127.0.0.1:8082/portal)

![marry-home-01](img/marry-home-01.png)

### Bob sends his medical record to Mary

Bob can now head over to his file list and click on __send__.

![bob-send-01](img/bob-send-01.png)

### Mary gets Bob's medical record

After a while Mary's wallet sees the tranasaction on the Nessus network and Mary's IPFS node will be able to see the IPFS file.

![marry-list-01](img/marry-list-01.png)

### Mary decrypts Bob's medical record

Mary (and only Mary) can now get/decrypt that IPFS file.

![marry-list-02](img/marry-list-02.png)

### Mary takes a look at the content

The previous __get__ has first transferred the encrypted file to Mary's local storage and then decrypted it.
Marr can now click __show__ on the decrypted file to view the content.

![marry-show-01](img/marry-show-01.png)

### Finally

Thanks for watching this demo, perhaps you liked it.

