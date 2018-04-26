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

Install the contents of its bin subdirectory into the /usr/bin directory.

```
sudo install -m 0755 -o root -g root -t /usr/bin bitcoin-$BTCVER/bin/*
```
    
Install the the bitcoin deamon as a service.

```
wget https://raw.githubusercontent.com/bitcoin/bitcoin/v$BTCVER/contrib/init/bitcoind.service
sed -i "s/bitcoind -daemon/bitcoind -regtest -daemon/" bitcoind.service 

sudo install -m 0644 -o root -g root -t /lib/systemd/system bitcoind.service

sudo systemctl daemon-reload
sudo systemctl list-unit-files bitcoind.*
sudo systemctl enable bitcoind
sudo systemctl start bitcoind
```
    
## Mine a few coins

```
bitcoin-cli -regtest generate 500
bitcoin-cli -regtest getbalance
```    
    
    