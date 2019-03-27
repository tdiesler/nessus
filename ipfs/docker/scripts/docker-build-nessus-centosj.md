## Build the Java base image

```
rm -rf docker
mkdir -p docker

export NVERSION=7

cat << EOF > docker/Dockerfile
FROM centos:$NVERSION

RUN yum -y install java-1.8.0-openjdk

ENTRYPOINT ["java"]
EOF

docker build -t nessusio/centosj docker/
docker run --rm nessusio/centosj -version

docker push nessusio/centosj

docker tag nessusio/centosj nessusio/centosj:$NVERSION
docker push nessusio/centosj:$NVERSION
```

