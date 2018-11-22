## Build the Nessus WebUI image

```
rm -rf docker
mkdir -p docker

export NVERSION=29

cat << EOF > docker/Dockerfile
FROM fedora:$NVERSION

RUN dnf -y install java-1.8.0-openjdk

ENTRYPOINT ["java"]
EOF

docker build -t nessusio/fedoraj docker/
docker push nessusio/fedoraj

docker run --rm nessusio/fedoraj -version

docker tag nessusio/fedoraj nessusio/fedoraj:$NVERSION
docker push nessusio/fedoraj:$NVERSION
```

