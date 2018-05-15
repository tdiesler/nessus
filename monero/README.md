Setup a Monero Node
--------------------

[Running A Full Node](https://getmonero.org/resources/user-guides/vps_run_node.html)

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
ssh root@xmrmn

export NUSER=xmr
useradd -G root $NUSER
cp -r .ssh /home/$NUSER
chown -R $NUSER.$NUSER /home/$NUSER/.ssh

yum update -y
yum install -y wget bzip2

timedatectl set-timezone Europe/Amsterdam
timedatectl 
```

## Download and Install Monero

```
ssh xmrusr@xmrmn

export XMRVER=v0.12.0.0
wget https://downloads.getmonero.org/cli/monero-linux-x64-$XMRVER.tar.bz2
bunzip2 monero-linux-x64-$XMRVER.tar.bz2 
tar xf monero-linux-x64-$XMRVER.tar
ln -s monero-$XMRVER monero
```

Install binaries into the /usr/bin directory.

```
sudo install -m 0755 -o root -g root -t /usr/bin monero/monerod
```

Set some monero deamon config option 
    
```
export RPC_BIND_IP=206.189.8.59
export RPC_LOGIN=rpcusr:rpcpass
echo rpc-bind-ip=$RPC_BIND_IP > monerod.conf
echo rpc-login=$RPC_LOGIN >> monerod.conf
echo confirm-external-bind=1 >> monerod.conf
sudo mv monerod.conf /etc
```

Install the the monero deamon as a service.

```
wget https://raw.githubusercontent.com/monero-project/monero/master/utils/systemd/monerod.service
sed -i "s/User=monero/User=$USER/" monerod.service
sed -i "s/Group=monero/Group=$USER/" monerod.service

sudo install -m 0644 -o root -g root -t /lib/systemd/system monerod.service
sudo chmod 777 /run

sudo systemctl daemon-reload
sudo systemctl list-unit-files monerod.*
sudo systemctl enable monerod
sudo systemctl start monerod
```
        