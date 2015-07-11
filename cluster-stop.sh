#!/bin/sh

NODES=`docker ps -a | grep cassandra- | wc -l`

for i in `seq 1 $NODES`;
do
    docker rm -f "cassandra-$i"
done
