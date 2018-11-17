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

if [[ ! -z "${IPFS_ENV_GATEWAY_ADDR}" ]] && [[ ! -z "${IPFS_PORT_5001_TCP_PORT}" ]]
then
    export IPFS_GATEWAY_ADDR="$IPFS_ENV_GATEWAY_ADDR"
    export IPFS_GATEWAY_PORT="$IPFS_PORT_5001_TCP_PORT"
fi

if [[ ! -z "${JAXRS_PORT_8081_TCP_ADDR}" ]] && [[ ! -z "${JAXRS_PORT_8081_TCP_PORT}" ]]
then
    export NESSUS_JAXRS_ADDR="$JAXRS_PORT_8081_TCP_ADDR"
    export NESSUS_JAXRS_PORT="$JAXRS_PORT_8081_TCP_PORT"
fi

export BLOCKCHAIN_CLASS_NAME="io.nessus.bitcoin.BitcoinBlockchain"

if [[ ! -z "${BLOCKCHAIN_PORT_18332_TCP_ADDR}" ]] && [[ ! -z "${BLOCKCHAIN_PORT_18332_TCP_PORT}" ]]
then
    export BLOCKCHAIN_JSONRPC_ADDR="$BLOCKCHAIN_PORT_18332_TCP_ADDR"
    export BLOCKCHAIN_JSONRPC_PORT="$BLOCKCHAIN_PORT_18332_TCP_PORT"
fi
if [[ ! -z "${BLOCKCHAIN_ENV_RPCUSER}" ]] && [[ ! -z "${BLOCKCHAIN_ENV_RPCPASS}" ]]
then
    export BLOCKCHAIN_JSONRPC_USER="$BLOCKCHAIN_ENV_RPCUSER"
    export BLOCKCHAIN_JSONRPC_PASS="$BLOCKCHAIN_ENV_RPCPASS"
fi

JAVA_OPTS="-Xmx200m"

java $JAVA_OPTS -Dlog4j.configuration=file://$HOMEDIR/config/log4j.properties \
     -jar $HOMEDIR/lib/nessus-ipfs-webui-@project.version@.jar $@
