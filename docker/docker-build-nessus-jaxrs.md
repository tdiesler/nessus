## Build the JAXRS image

```
export NVERSION=1.0.0-SNAPSHOT

rm -rf docker
mkdir -p docker

tar xzf nessus-ipfs-dist-$NVERSION-deps.tgz -C docker
tar xzf nessus-ipfs-dist-$NVERSION-proj.tgz -C docker

cat << EOF > docker/Dockerfile
FROM nessusio/fedoraj:1.8.0

# Install the binaries
COPY nessus-ipfs-dist-$NVERSION nessus-ipfs-jaxrs

# Make the entrypoint executable
RUN ln -s /nessus-ipfs-jaxrs/bin/run-nessus-jaxrs.sh /usr/local/bin/nessus-jaxrs

# Expose the JAXRS port
EXPOSE 8081

ENTRYPOINT ["nessus-jaxrs"]
EOF

docker build -t nessusio/ipfs-jaxrs docker/
docker push nessusio/ipfs-jaxrs

docker tag nessusio/ipfs-jaxrs nessusio/ipfs-jaxrs:$NVERSION
docker push nessusio/ipfs-jaxrs:$NVERSION
```

### Run the JAXRS image

```
export CNAME=jaxrs

docker rm -f $CNAME
docker run --detach \
    --link ipfs:ipfs \
    --link btcd:blockchain \
    --memory=100m --memory-swap=2g \
    --name $CNAME \
    nessusio/ipfs-jaxrs

# Follow the info log
docker logs -f jaxrs

# Follow the info log on the journal
journalctl CONTAINER_NAME=jaxrs -f

# Follow the debug log
docker exec -it jaxrs tail -f -n 100 debug.log
```