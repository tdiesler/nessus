
## Vultr

* CentOS 7
* 1 vCPU
* 1 GB RAM
* 25 GB SSD

```
ssh root@vps

sed -i "s/PasswordAuthentication yes/PasswordAuthentication no/" /etc/ssh/sshd_config
cat /etc/ssh/sshd_config | grep PasswordAuthentication
systemctl restart sshd

dnf -y update

timedatectl set-timezone Europe/Amsterdam
timedatectl

export NUSER=bob
useradd -G root,wheel -m $NUSER -s /bin/bash
cp -r .ssh /home/$NUSER/
chown -R $NUSER.$NUSER /home/$NUSER/.ssh

cat << EOF > /etc/sudoers.d/user-privs-$NUSER
$NUSER ALL=(ALL:ALL) NOPASSWD: ALL
EOF
```

###  Swap setup to avoid running out of memory

```
fallocate -l 4G /mnt/swapfile
dd if=/dev/zero of=/mnt/swapfile bs=1024 count=4M
mkswap /mnt/swapfile
chmod 600 /mnt/swapfile
swapon /mnt/swapfile
echo '/mnt/swapfile none swap sw 0 0' >> /etc/fstab
free -h
```

### Download and Install Docker

```
dnf -y install docker

systemctl daemon-reload
systemctl enable docker
systemctl restart docker

docker ps
```
