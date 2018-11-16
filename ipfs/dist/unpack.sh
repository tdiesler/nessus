#!/bin/bash

PRG="$0"

HOMEDIR=`dirname $PRG`

# Get absolute path of the HOMEDIR
CURDIR=`pwd`
HOMEDIR=`cd $HOMEDIR; pwd`
cd $CURDIR

cd $HOMEDIR/target
for f in `ls *.tgz`;
do
    echo $f
    tar xzf $f
done
