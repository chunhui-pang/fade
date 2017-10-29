#!/bin/bash

DDIR=../data/$(date +%Y-%m-%d)
if [ "$1" != "" ]; then
    DDIR=$1/$(date +%Y-%m-%d)
fi
if [ -d $DDIR ]; then
    echo Output directory "$DDIR" has exists, exit...
    exit 1
fi

echo retrieve fibs...
./retrieve-fib.sh ../data/

echo retrieve interface infos...
./retrieve-interfaces.sh ../data/
# nodejs retrieve-interfaces.js > $DDIR/interfaces
echo Retrieving data are saved to $DDIR!
