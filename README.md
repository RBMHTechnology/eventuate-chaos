Eventuate chaos testing utilities
=================================

This is very early work in progress on chaos testing utilities for [Eventuate](https://github.com/RBMHTechnology/eventuate) and [Apache Cassandra](http://cassandra.apache.org/). They should help developers running Cassandra clusters and Eventuate applications on a single Mac OS X machine with [Docker](https://www.docker.com/) and generate failures by randomly stopping and restarting containers.

Prerequisites
-------------

### Installed Software

- [VirtualBox](https://www.virtualbox.org/wiki/Downloads) 4.3.28 or higher
- [boot2docker](http://boot2docker.io/) 1.6.2 or higher
- [sbt](http://www.scala-sbt.org/download.html) 0.13.x or higher

### Custom routing

By default, docker containers running in the boot2docker VM are not directly accessible from Mac OS. To make them accessible, packets targeted at the docker container IP addresses must be routed to the boot2docker VM. The approach taken here was inspired by the article [Accessing docker container private network easily from your boot2docker host](http://ispyker.blogspot.de/2014/04/accessing-docker-container-private.html) and has been slightly modified. Assuming that Docker containers have IP addresses `172.17.x.x`, a route to the boot2docker VM is added with: 

    almdudler:~ martin$ sudo route -n add 172.17.0.0/16 `boot2docker ip`
    add net 172.17.0.0: gateway 192.168.59.103

Running a Cassandra cluster
---------------------------

For starting a Cassandra cluster with four nodes, run:

    almdudler:~ martin$ ./cluster-start.sh 4
    cassandra-1
    cassandra-2
    cassandra-3
    cassandra-4

If no argument is given, three nodes are started by default. Running the command for the first time downloads all required Docker images which may take a while. For stopping the cluster and removing all containers, run: 
 
    almdudler:~ martin$ ./cluster-stop.sh 
    cassandra-1
    cassandra-2
    cassandra-3
    cassandra-4

Running a Cassandra cluster with random node failures
-----------------------------------------------------

For running a cluster with random node failures, start the [`ChaosCluster`](https://github.com/RBMHTechnology/eventuate-chaos/blob/master/src/main/scala/com/rbmhtechnology/eventuate/chaos/ChaosCluster.scala) application from the sbt command prompt:

    almdudler:~ martin$ sbt
    [info] Loading global plugins from /Users/martin/.sbt/0.13/plugins
    [info] Loading project definition from /Users/martin/eventuate-chaos/project
    [info] Set current project to eventuate-chaos (in build file:/Users/martin/eventuate-chaos/)
    > runMain com.rbmhtechnology.eventuate.chaos.ChaosCluster
    [info] Running com.rbmhtechnology.eventuate.chaos.ChaosCluster 
    Writing /Users/martin/.boot2docker/certs/boot2docker-vm/ca.pem
    Writing /Users/martin/.boot2docker/certs/boot2docker-vm/cert.pem
    Writing /Users/martin/.boot2docker/certs/boot2docker-vm/key.pem
    cassandra-1
    cassandra-2
    cassandra-3
    cassandra-4
    Cluster started. Press any key to start chaos ...

By default, a four node cluster is started (see also [reference.conf](https://github.com/RBMHTechnology/eventuate-chaos/blob/master/src/main/resources/reference.conf) for further details). Pressing any key after the cluster started generates chaos by randomly stopping and (re)starting nodes: 

    cassandra-4
    cassandra-2
    node(s) stopped. Press any key to stop cluster ...
    cassandra-4
    cassandra-2
    Node(s) started. Press any key to stop cluster ...
    cassandra-3
    node(s) stopped. Press any key to stop cluster ...
    cassandra-3
    Node(s) started. Press any key to stop cluster ...

Pressing any key a second time stops the cluster and removes all containers:

    cassandra-1
    cassandra-2
    cassandra-3
    cassandra-4
    Cluster stopped
    [success] Total time: 77 s, completed 11.07.2015 17:12:22

Accessing the cluster seed node
-------------------------------

From the command line, the IP address of the cluster seed node (`cassandra-1`) can be obtained with: 

    almdudler:~ martin$ docker inspect --format='{{ .NetworkSettings.IPAddress }}' cassandra-1
    172.17.0.1

Programmatically, applications can use the [`ChaosCommands`](https://github.com/RBMHTechnology/eventuate-chaos/blob/master/src/main/scala/com/rbmhtechnology/eventuate/chaos/ChaosCluster.scala) trait and obtain the seed node `InetAddress` via the `seedAddress` method: 

```scala
def seedAddress(): Try[InetAddress]
```
