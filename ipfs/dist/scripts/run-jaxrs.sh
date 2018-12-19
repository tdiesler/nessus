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

#######################################################
#
# IPSF API CONFIG
#
if [[ -z "${IPFS_JSONRPC_ADDR}" ]]; then
    export IPFS_JSONRPC_ADDR="$IPFS_PORT_5001_TCP_ADDR"
fi
if [[ -z "${IPFS_JSONRPC_PORT}" ]]; then
    export IPFS_JSONRPC_PORT="$IPFS_PORT_5001_TCP_PORT"
fi

#######################################################
#
# BLOCKCHAIN API CONFIG
#
if [[ -z "${BLOCKCHAIN_CLASS_NAME}" ]]; then
    export BLOCKCHAIN_CLASS_NAME="io.nessus.bitcoin.BitcoinBlockchain"
fi
if [[ -z "${BLOCKCHAIN_JSONRPC_ADDR}" ]]; then
    export BLOCKCHAIN_JSONRPC_ADDR="$BLOCKCHAIN_PORT_18332_TCP_ADDR"
fi
if [[ -z "${BLOCKCHAIN_JSONRPC_PORT}" ]]; then
    export BLOCKCHAIN_JSONRPC_PORT="$BLOCKCHAIN_PORT_18332_TCP_PORT"
fi
if [[ -z "${BLOCKCHAIN_JSONRPC_USER}" ]]; then
    export BLOCKCHAIN_JSONRPC_USER="$BLOCKCHAIN_ENV_RPCUSER"
fi
if [[ -z "${BLOCKCHAIN_JSONRPC_PASS}" ]]; then
    export BLOCKCHAIN_JSONRPC_PASS="$BLOCKCHAIN_ENV_RPCPASS"
fi

#######################################################
#
# JAXRS API CONFIG
#
if [[ -z "${NESSUS_JAXRS_ADDR}" ]]; then
    export NESSUS_JAXRS_ADDR="0.0.0.0"
fi
if [[ -z "${NESSUS_JAXRS_PORT}" ]]; then
    export NESSUS_JAXRS_PORT="8081"
fi

# DEBUG LOG
#
#echo "BLOCKCHAIN:   $BLOCKCHAIN_JSONRPC_USER:$BLOCKCHAIN_JSONRPC_PASS@$BLOCKCHAIN_JSONRPC_ADDR:$BLOCKCHAIN_JSONRPC_PORT"
#echo "IPFS_JSONRPC: $IPFS_JSONRPC_ADDR:$IPFS_JSONRPC_PORT"
#echo "NESSUS_JAXRS: $NESSUS_JAXRS_ADDR:$NESSUS_JAXRS_PORT"

JAVA_OPTS="-Xmx200m"

java $JAVA_OPTS -Dlog4j.configuration=file://$HOMEDIR/config/log4j.properties \
     -jar $HOMEDIR/lib/nessus-ipfs-jaxrs-@project.version@.jar $@
