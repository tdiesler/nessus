## Build the Nessus WebUI image

```
rm -rf docker
mkdir -p docker

cat << EOF > docker/Dockerfile
FROM fedora:29

RUN dnf -y install java-1.8.0-openjdk

ENTRYPOINT ["java"]
EOF

docker build -t nessusio/fedoraj docker/
docker push nessusio/fedoraj

docker run --rm nessusio/fedoraj -version

docker tag nessusio/fedoraj nessusio/fedoraj:1.8.0
docker push nessusio/fedoraj:1.8.0
```

