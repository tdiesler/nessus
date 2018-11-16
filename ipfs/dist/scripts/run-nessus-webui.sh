#!/bin/sh

PRG="$0"

# need this for relative symlinks
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG="`dirname "$PRG"`/$link"
  fi
done

HOMEDIR=`dirname $PRG`/..

# Get absolute path of the HOMEDIR
CURDIR=`pwd`
HOMEDIR=`cd $HOMEDIR; pwd`
cd $CURDIR

export BLOCKCHAIN_CLASS_NAME="io.nessus.bitcoin.BitcoinBlockchain"

if [[ ! -z "${IPFS_ENV_GATEWAYIP}" ]] && [[ ! -z "${IPFS_PORT_8080_TCP_PORT}" ]]
then
    export IPFS_GATEWAY_HOST="$IPFS_ENV_GATEWAYIP"
    export IPFS_GATEWAY_PORT="$IPFS_PORT_8080_TCP_PORT"
fi

if [[ ! -z "${JAXRS_PORT_8081_TCP_ADDR}" ]] && [[ ! -z "${JAXRS_PORT_8081_TCP_PORT}" ]]
then
    export NESSUS_JAXRS_HOST="$JAXRS_PORT_8081_TCP_ADDR"
    export NESSUS_JAXRS_PORT="$JAXRS_PORT_8081_TCP_PORT"
fi


if [[ ! -z "${JAXRS_ENV_BLOCKCHAIN_JSONRPC_ADDR}" ]] && [[ ! -z "${JAXRS_ENV_BLOCKCHAIN_JSONRPC_PORT}" ]]
then
    export BLOCKCHAIN_JSONRPC_URL="$JAXRS_ENV_BLOCKCHAIN_JSONRPC_ADDR:$JAXRS_ENV_BLOCKCHAIN_JSONRPC_PORT"
fi

if [[ ! -z "${JAXRS_ENV_BLOCKCHAIN_JSONRPC_USER}" ]] && [[ ! -z "${JAXRS_ENV_BLOCKCHAIN_JSONRPC_PASS}" ]]
then
    export BLOCKCHAIN_JSONRPC_USER="$JAXRS_ENV_BLOCKCHAIN_JSONRPC_USER"
    export BLOCKCHAIN_JSONRPC_PASS="$JAXRS_ENV_BLOCKCHAIN_JSONRPC_PASS"
fi

JAVA_OPTS="-Xmx200m"

java $JAVA_OPTS -Dlog4j.configuration=file://$HOMEDIR/config/log4j.properties \
     -jar $HOMEDIR/lib/nessus-ipfs-webui-@project.version@.jar $@
