Setup a Bitcoin Node
--------------------

[Running A Full Node](https://bitcoin.org/en/full-node)

## Minimum Requirements

* 145 GB of free disk space, accessable at a minimum read/write speed of 100 MB/s.
* 2 GB of memory (RAM)
* A broadband Internet connection with upload speeds of at least 400 kilobits (50 kilobytes) per second

## Digital Ocean

* Fedora 27 x64
* 1 vCPU
* 2 GB RAM
* 50 GB SSD

```
ssh root@btctn

export NUSER=bitcoin
sudo useradd -G root $NUSER
cp -r .ssh /home/$NUSER
chown -R $NUSER.$NUSER /home/$NUSER/.ssh

sudo yum update -y
sudo yum install -y wget

sudo timedatectl set-timezone Europe/Amsterdam
timedatectl 
```

## Download and Install Bitcoin Core

```
ssh bitcoin@btctn

export BTCVER=0.16.0
wget https://bitcoin.org/bin/bitcoin-core-$BTCVER/bitcoin-$BTCVER-x86_64-linux-gnu.tar.gz
tar xzf bitcoin-$BTCVER-x86_64-linux-gnu.tar.gz 
```

Install some bitcoin binaries into the /usr/bin directory.

```
sudo install -m 0755 -o root -g root -t /usr/bin bitcoin-$BTCVER/bin/bitcoind
sudo install -m 0755 -o root -g root -t /usr/bin bitcoin-$BTCVER/bin/bitcoin-cli
```

Set some bitcoin deamon config option 
    
```
sudo vi /etc/bitcoin/bitcoin.conf

server=1
regtest=1
rpcuser=regusr
rpcpassword=regpass
rpcbind=127.0.0.1
rpcallowip=127.0.0.1
rpcbind=192.168.178.2
rpcallowip=192.168.178.2/255.255.255.0
#rpcport=18443
```

Install the the bitcoin deamon as a service.

```
wget https://raw.githubusercontent.com/bitcoin/bitcoin/v$BTCVER/contrib/init/bitcoind.service
cat bitcoind.service

sudo install -m 0644 -o root -g root -t /lib/systemd/system bitcoind.service

sudo systemctl daemon-reload
sudo systemctl list-unit-files bitcoind.*
sudo systemctl enable bitcoind
sudo systemctl start bitcoind
```
    
## Get a few test coins

```
bitcoin-cli -testnet getbalance
0.00000000

bitcoin-cli -testnet getaccountaddress ""
2N89LPCNPNTDp9DQBkvgttDzW2HBaxvUHCC

https://testnet.coinfaucet.eu/en
https://testnet.blockexplorer.com/address/2N89LPCNPNTDp9DQBkvgttDzW2HBaxvUHCC
```

    
## Bitcoin command line API

```
[bitcoin@btctn-01 ~]$ bitcoin-cli -regtest help
== Blockchain ==
getbestblockhash
getblock "blockhash" ( verbosity ) 
getblockchaininfo
getblockcount
getblockhash height
getblockheader "hash" ( verbose )
getchaintips
getchaintxstats ( nblocks blockhash )
getdifficulty
getmempoolancestors txid (verbose)
getmempooldescendants txid (verbose)
getmempoolentry txid
getmempoolinfo
getrawmempool ( verbose )
gettxout "txid" n ( include_mempool )
gettxoutproof ["txid",...] ( blockhash )
gettxoutsetinfo
preciousblock "blockhash"
pruneblockchain
savemempool
verifychain ( checklevel nblocks )
verifytxoutproof "proof"

== Control ==
getmemoryinfo ("mode")
help ( "command" )
logging ( <include> <exclude> )
stop
uptime

== Generating ==
generate nblocks ( maxtries )
generatetoaddress nblocks address (maxtries)

== Mining ==
getblocktemplate ( TemplateRequest )
getmininginfo
getnetworkhashps ( nblocks height )
prioritisetransaction <txid> <dummy value> <fee delta>
submitblock "hexdata"  ( "dummy" )

== Network ==
addnode "node" "add|remove|onetry"
clearbanned
disconnectnode "[address]" [nodeid]
getaddednodeinfo ( "node" )
getconnectioncount
getnettotals
getnetworkinfo
getpeerinfo
listbanned
ping
setban "subnet" "add|remove" (bantime) (absolute)
setnetworkactive true|false

== Rawtransactions ==
combinerawtransaction ["hexstring",...]
createrawtransaction [{"txid":"id","vout":n},...] {"address":amount,"data":"hex",...} ( locktime ) ( replaceable )
decoderawtransaction "hexstring" ( iswitness )
decodescript "hexstring"
fundrawtransaction "hexstring" ( options iswitness )
getrawtransaction "txid" ( verbose "blockhash" )
sendrawtransaction "hexstring" ( allowhighfees )
signrawtransaction "hexstring" ( [{"txid":"id","vout":n,"scriptPubKey":"hex","redeemScript":"hex"},...] ["privatekey1",...] sighashtype )

== Util ==
createmultisig nrequired ["key",...]
estimatefee nblocks
estimatesmartfee conf_target ("estimate_mode")
signmessagewithprivkey "privkey" "message"
validateaddress "address"
verifymessage "address" "signature" "message"

== Wallet ==
abandontransaction "txid"
abortrescan
addmultisigaddress nrequired ["key",...] ( "account" "address_type" )
backupwallet "destination"
bumpfee "txid" ( options ) 
dumpprivkey "address"
dumpwallet "filename"
encryptwallet "passphrase"
getaccount "address"
getaccountaddress "account"
getaddressesbyaccount "account"
getbalance ( "account" minconf include_watchonly )
getnewaddress ( "account" "address_type" )
getrawchangeaddress ( "address_type" )
getreceivedbyaccount "account" ( minconf )
getreceivedbyaddress "address" ( minconf )
gettransaction "txid" ( include_watchonly )
getunconfirmedbalance
getwalletinfo
importaddress "address" ( "label" rescan p2sh )
importmulti "requests" ( "options" )
importprivkey "privkey" ( "label" ) ( rescan )
importprunedfunds
importpubkey "pubkey" ( "label" rescan )
importwallet "filename"
keypoolrefill ( newsize )
listaccounts ( minconf include_watchonly)
listaddressgroupings
listlockunspent
listreceivedbyaccount ( minconf include_empty include_watchonly)
listreceivedbyaddress ( minconf include_empty include_watchonly)
listsinceblock ( "blockhash" target_confirmations include_watchonly include_removed )
listtransactions ( "account" count skip include_watchonly)
listunspent ( minconf maxconf  ["addresses",...] [include_unsafe] [query_options])
listwallets
lockunspent unlock ([{"txid":"txid","vout":n},...])
move "fromaccount" "toaccount" amount ( minconf "comment" )
removeprunedfunds "txid"
rescanblockchain ("start_height") ("stop_height")
sendfrom "fromaccount" "toaddress" amount ( minconf "comment" "comment_to" )
sendmany "fromaccount" {"address":amount,...} ( minconf "comment" ["address",...] replaceable conf_target "estimate_mode")
sendtoaddress "address" amount ( "comment" "comment_to" subtractfeefromamount replaceable conf_target "estimate_mode")
setaccount "address" "account"
settxfee amount
signmessage "address" "message"
walletlock
walletpassphrase "passphrase" timeout
walletpassphrasechange "oldpassphrase" "newpassphrase"
```

    
    