#!/bin/sh

if [ -n "$1" ]; then
    NODES=$1
else
    NODES=3
fi

docker run --name cassandra-1 -d cassandra:2.1.6 > /dev/null
echo "cassandra-1"

SEED=`docker inspect --format='{{ .NetworkSettings.IPAddress }}' cassandra-1`

if [ $NODES -gt 1 ]; then
    for i in `seq 2 $NODES`;
    do
        docker run --name "cassandra-$i" -d -e CASSANDRA_SEEDS=$SEED cassandra:2.1.6 > /dev/null
        echo "cassandra-$i"
    done
fi
