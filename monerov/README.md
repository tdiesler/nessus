Setup a MoneroV Node
--------------------

[Running A Full Node](https://github.com/monerov/monerov#compiling-monerov-from-source)

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
ssh root@xmr01

export NUSER=xmv
useradd -G root $NUSER
cp -r .ssh /home/$NUSER
chown -R $NUSER.$NUSER /home/$NUSER/.ssh

yum update -y

timedatectl set-timezone Europe/Amsterdam
timedatectl 
```

## Build and Install MoneroV

[Compiling MoneroV from source](https://github.com/monerov/monerov#compiling-monerov-from-source)

```
ssh xmv@xmr01

sudo yum install -y git make gcc gcc-c++ cmake pkgconf boost-devel openssl-devel cppzmq-devel unbound-devel libsodium-devel
```

Clone recursively to pull-in needed submodule(s):

```
mkdir git; cd git
git clone --recursive https://github.com/monerov/monerov.git
```

If you already have a repo cloned, initialize and update:

```
cd git/monerov && git submodule init && git submodule update
```

Build the the binaries 

```
make CXXFLAGS="-Wno-error=class-memaccess" CFLAGS="-Wno-error=class-memaccess"
```

Copy the release binaries 

```
cp -r build/release/bin ~/monerov-v0.13.0.0-SNAPSHOT
```

Install binaries into the /usr/bin directory.

```
ln -s monerov-v0.13.0.0-SNAPSHOT monerov
sudo install -m 0755 -o root -g root -t /usr/bin monerov/monerovd
```

Set some monerov deamon config option 
    
```
export RPC_BIND_IP=206.189.8.59
export RPC_LOGIN=rpcusr:rpcpass
echo rpc-bind-ip=$RPC_BIND_IP > monerovd.conf
echo rpc-login=$RPC_LOGIN >> monerovd.conf
echo confirm-external-bind=1 >> monerovd.conf
sudo mv monerovd.conf /etc
```

Install the the monero deamon as a service.

```
wget https://raw.githubusercontent.com/monerov/monerov/master/utils/systemd/monerod.service
sed -i "s/User=monerov/User=$USER/" monerod.service
sed -i "s/Group=monerov/Group=$USER/" monerod.service
mv monerod.service monerovd.service

sudo install -m 0644 -o root -g root -t /lib/systemd/system monerovd.service
sudo chmod 777 /run

sudo systemctl daemon-reload
sudo systemctl list-unit-files monerovd.*
sudo systemctl enable monerovd
sudo systemctl start monerovd
```



