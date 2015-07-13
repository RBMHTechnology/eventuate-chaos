Eventuate chaos testing utilities
=================================

This is very early work in progress on chaos testing utilities for [Eventuate](https://github.com/RBMHTechnology/eventuate) and [Apache Cassandra](http://cassandra.apache.org/). They support running Cassandra clusters and Eventuate applications on a single Mac OS X machine with [Docker](https://www.docker.com/) and generate failures by randomly stopping and restarting containers.

Prerequisites
-------------

### Installed Software

- [boot2docker](http://boot2docker.io/) 1.6.2 or higher
- [sbt](http://www.scala-sbt.org/download.html) 0.13.x or higher

### Started boot2docker VM

    almdudler:eventuate-chaos martin$ boot2docker start
    Waiting for VM and Docker daemon to start...
    ..........ooooooooo
    Started.

### Custom route

By default, docker containers, running in the boot2docker VM, are not directly accessible from Mac OS. To make them accessible, packets targeted at docker container IP addresses must be routed to the boot2docker VM. Assuming that Docker containers have IP addresses `172.17.x.x`, a route to the boot2docker VM is added with: 

    almdudler:eventuate-chaos martin$ sudo route -n add 172.17.0.0/16 `boot2docker ip`
    add net 172.17.0.0: gateway 192.168.59.103

Running a Cassandra cluster
---------------------------

### Manual failure generation

One way of running a local Cassandra cluster is using the scripts `cluster-start.sh` and `cluster-stop.sh`. These scripts and other `docker` commands below require a shell to be initialized with: 

    almdudler:eventuate-chaos martin$ eval `boot2docker shellinit`
  
For starting a Cassandra cluster with four nodes, for example, run:

    almdudler:eventuate-chaos martin$ ./cluster-start.sh 4
    cassandra-1
    cassandra-2
    cassandra-3
    cassandra-4

Running the command for the first time downloads all required Docker images which may take a while. If no argument is given, three nodes are started by default. The script prints the names of the started containers (`cassandra-<i>`) each containing a cluster node. For stopping node three, for example, run: 

    almdudler:eventuate-chaos martin$ docker stop cassandra-3
    cassandra-3

For sending node three a `SIGKILL` instead, run:

    almdudler:eventuate-chaos martin$ docker kill cassandra-3
    cassandra-3

For starting that node again, run:

    almdudler:eventuate-chaos martin$ docker start cassandra-3
    cassandra-3

For stopping the whole cluster and removing all containers, run: 
 
    almdudler:eventuate-chaos martin$ ./cluster-stop.sh 
    cassandra-1
    cassandra-2
    cassandra-3
    cassandra-4

### Random failure generation

For running a cluster with random node failures, start the [`ChaosCluster`](https://github.com/RBMHTechnology/eventuate-chaos/blob/master/src/main/scala/com/rbmhtechnology/eventuate/chaos/ChaosCluster.scala) application with sbt:

    almdudler:eventuate-chaos martin$ sbt
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

By default, a four node cluster is started (see also [reference.conf](https://github.com/RBMHTechnology/eventuate-chaos/blob/master/src/main/resources/reference.conf) for further details). Pressing any key, after the cluster started, generates chaos by randomly stopping and (re)starting nodes, except the seed node (`cassandra-1`): 

    cassandra-4
    cassandra-2
    Node(s) stopped. Press any key to stop cluster ...
    cassandra-4
    cassandra-2
    Node(s) started. Press any key to stop cluster ...
    cassandra-3
    Node(s) stopped. Press any key to stop cluster ...
    cassandra-3
    Node(s) started. Press any key to stop cluster ...

Pressing any key a second time stops the cluster and removes all containers:

    cassandra-1
    cassandra-2
    cassandra-3
    cassandra-4
    Cluster stopped
    [success] Total time: 77 s, completed 11.07.2015 17:12:22

Obtaining the seed node address
-------------------------------

From the command line, the IP address of the cluster seed node (`cassandra-1`) can be obtained with: 

    almdudler:eventuate-chaos martin$ docker inspect --format='{{ .NetworkSettings.IPAddress }}' cassandra-1
    172.17.0.1

In Scala applications, the `seedAddress` method of the [`ChaosCommands`](https://github.com/RBMHTechnology/eventuate-chaos/blob/master/src/main/scala/com/rbmhtechnology/eventuate/chaos/ChaosCommands.scala) trait can be used to obtain the `InetAddress` of the seed node: 

```scala
def seedAddress(): Try[InetAddress]
```
