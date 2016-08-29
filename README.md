Eventuate chaos testing utilities
=================================

This is very early work in progress on chaos testing utilities for [Eventuate][eventuate] and [Apache
Cassandra](http://cassandra.apache.org/). They support running Cassandra clusters and Eventuate applications with
[Docker][docker] and using [blockade][blockade] to easily generate failures like stopping and restarting of containers
and introducing network failures such as partitions, packet loss and slow connections.

This repository can be seen as a toolkit or collection of utilities which you can use to test your Eventuate
applications. Moreover we are going to describe an examplary test setup that gives an introduction into these tools and
serves as a blueprint to build your own more complex test scenarios.


##### Blockade

The test scenarios we are about to create mainly facilitate the [blockade][blockade] tool. Blockade is a utility for
testing network failures and partitions in distributed applications. Blockade uses Docker containers to run
application processes and manages the network from the host system to create various failure scenarios. Docker itself is
a container engine that allows you to package and run applications in a hardware-agnostic and platform-agnostic fashion -
often called like somewhat *lightweight virtual machines*.

You can find further information on the [blockade github page][blockade] regarding its configuration and the
possibilities you have in addition to what is mentioned in here.


Prerequisites
-------------

#### Linux

- [Docker][docker] (tested with docker >= 1.6)
- [blockade][blockade] (`>= 0.2.0`, currently a fork of the original [dcm-oss/blockade](https://github.com/dcm-oss/blockade))

##### Initial setup

Given your Docker daemon is running and you have [blockade][blockade] in your `$PATH` you are basically ready to go. All
you have to do once is to pull or build the necessary Docker containers the test cluster is based on:

``` bash
# cassandra image
$ docker pull cassandra:2.2.3

# sbt + scala + java8 base image
$ docker build -t eventuate-chaos/sbt .
```

After that is done you can start the minimal docker DNS container that is used to identify the containers with each
other:

    $ ./start-dns.sh

This script will look for the default `docker0` network interface and start a `dnsdock` image on port 53 for DNS
discovery. You may have to specify the location to your *docker socket* via `$DOCKER_SOCKET` in case the script does not
find it by itself:

    $ DOCKER_SOCKET=/var/run/docker.sock ./start-dns.sh

These steps only have to be taken once for the initial bootstrapping.


#### Mac OS

- [Docker Toolbox][toolbox]
- [Vagrant][vagrant] (tested with 1.7.4)
- [VirtualBox](https://www.virtualbox.org/)


##### Initial setup

As Docker does not run natively on Mac OS you have to use an existing [docker machine](https://docs.docker.com/machine/)
setup and follow the steps under [Linux](#linux) or use the pre-configured [Vagrant][vagrant] image that ships with this
repository. If you choose the latter you just have to build the image by running `vagrant up` and ssh into your new
machine:

```bash
$ vagrant up
$ vagrant ssh

# inside your virtual machine you can find the repository contents
# mounted under /vagrant as usual
cd /vagrant
```


##### Docker toolbox

Although Mac recently got a *native* [Docker for Mac](https://docs.docker.com/docker-for-mac/) implementation you still
have to use the [Docker Toolbox][toolbox] (which interfaces VirtualBox) to use Blockade and therefore *eventuate-chaos*
itself. This is because Blockade uses the linux `iptables` and `tc` tools to establish and simulate network
interference.


Example test setup
------------------

The examplary test setup we are describing in the following consists of 2 basic components:

- **Eventuate test appliation**:

    This is the Eventuate application we are actually testing with. It is an
    [EventsourcedActor](http://rbmhtechnology.github.io/eventuate/user-guide.html#event-sourced-actors) that continually
    (every 2 seconds) emits an event that increments an internal counter by persisting it changes. You can find its
    implementation in [ChaosActor.scala](./src/test/scala/com/rbmhtechnology/eventuate/chaos/ChaosActor.scala). The
    container which the scala application is running in is named `location-1`.

- **Cassandra cluster**:

    This cassandra cluster contains 3 nodes each running in its own Docker container. The seed node `c1` is the
    initially started one the remaining nodes `c2` and `c3` connect with.


#### Configuration

The test setup described above is encoded in the `blockade.yml` YAML configuration file that is used by
[blockade][blockade] to manage the test cluster:

``` yaml
containers:
  c1:
    image: cassandra:2.2.3

  c2:
    image: cassandra:2.2.3
    start_delay: 60
    links:
      c1: "c1"
    environment:
      CASSANDRA_SEEDS: "c1.cassandra.docker"

  c3:
    image: cassandra:2.2.3
    start_delay: 60
    links:
      c1: "c1"
    environment:
      CASSANDRA_SEEDS: "c1.cassandra.docker"

  location-1:
    image: eventuate-chaos/sbt
    command: ["test:run-main com.rbmhtechnology.eventuate.chaos.ChaosActor -d"]
    volumes:
      "${PWD}": "/app"
    links: [ "c2", "c3" ]
    environment:
      CASSANDRA_NODES: "c1.cassandra.docker,c2.cassandra.docker,c3.cassandra.docker"
```


Running a Cassandra cluster
---------------------------

### blockade chaos cluster

Now that you have all prerequisites fulfilled you can start up the [blockade][blockade] test cluster as it is configured
in the `blockade.yml` shipped with this repository. From this point on you may freely experiment and modify all given
parameters and configurations as this is just a toolkit to get you started as quickly as possible.

> Note that all blockade commands require root permissions.

```bash
$ sudo blockade up
```

Depending on the cassandra version (> 2.1) you have to configure a reasonable startup delay for every cassandra node
like 60 seconds (see the
[documentation](https://docs.datastax.com/en/cassandra/2.1/cassandra/operations/ops_add_node_to_cluster_t.html) for
further information). This may increase the initial startup time a lot. Once all nodes are up and running you can
inspect your running cluster via `blockade status`:

```bash
$ sudo blockade status
NODE            CONTAINER ID    STATUS  IP              NETWORK    PARTITION
c1              8ccf90e8f76e    UP      172.17.0.3      NORMAL
c2              5c65f3ca62ec    UP      172.17.0.5      NORMAL
c3              4b630db6a19e    UP      172.17.0.4      NORMAL
location-1      bb8c89f615ef    UP      172.17.0.6      NORMAL
```

##### DNS

In case of errors please make sure the `dnsdock` container is running and has its entry in your `/etc/resolv.conf`. You
can use the `start-dns.sh` script to check:

``` bash
# start the DNS if not running
$ ./start-dns.sh

# make an entry in your /etc/resolv.conf if necessary
$ sudo ./start-dns.sh -f
```


### failures

From now on you may introduce any kind of failure the [blockade][blockade] tool supports.

##### partition one cassandra node

``` bash
$ sudo blockade partition c2
NODE            CONTAINER ID    STATUS  IP              NETWORK    PARTITION
c1              8ccf90e8f76e    UP      172.17.0.3      NORMAL     2
c2              5c65f3ca62ec    UP      172.17.0.5      NORMAL     1
c3              4b630db6a19e    UP      172.17.0.4      NORMAL     2
location-1      bb8c89f615ef    UP      172.17.0.6      NORMAL     2
```


##### partition two cassandra nodes

As you can see in the `PARTITION` column of the command output all cassandra nodes are separated from each other while
the test application (`location-1`) can reach the cassandra node `c2` only.

``` bash
$ sudo blockade partition c1 c3
NODE            CONTAINER ID    STATUS  IP              NETWORK    PARTITION
c1              8ccf90e8f76e    UP      172.17.0.3      NORMAL     1
c2              5c65f3ca62ec    UP      172.17.0.5      NORMAL     3
c3              4b630db6a19e    UP      172.17.0.4      NORMAL     2
location-1      bb8c89f615ef    UP      172.17.0.6      NORMAL     3
```


##### flaky network connection of the Eventuate node

``` bash
$ sudo blockade flaky location-1
NODE            CONTAINER ID    STATUS  IP              NETWORK    PARTITION
c1              8ccf90e8f76e    UP      172.17.0.3      NORMAL     1
c2              5c65f3ca62ec    UP      172.17.0.5      NORMAL     3
c3              4b630db6a19e    UP      172.17.0.4      NORMAL     2
location-1      bb8c89f615ef    UP      172.17.0.6      FLAKY      3
```

##### restart of nodes

You may also restart one or multiple nodes and inspect the effect on the Eventuate application as well:

    $ sudo blockade restart c2 c3

#### Inspect test application output

While playing with the conditions of the test cluster you can see the Eventuate application output its current state
(being the actual counter value) on stdout. This is my personal preference but I like to inspect the current Eventuate
node's status in a [tmux](https://tmux.github.io/) split window while I modify the cluster's condition with `tmux
split-window "docker logs -f location-1"`:


```
counter = 65 (recovery = false)
counter = 66 (recovery = false)
persist failure 696: Not enough replica available for query at consistency QUORUM (2 required but only 1 alive)
persist failure 697: Not enough replica available for query at consistency QUORUM (2 required but only 1 alive)
counter = 67 (recovery = false)
...
counter = 74 (recovery = false)
counter = 75 (recovery = false)
persist failure 698: Cassandra timeout during write query at consistency QUORUM (2 replica were required but only 1 acknowledged the write)
counter = 76 (recovery = false)
counter = 77 (recovery = false)
```


#### Expected behavior

Just to recapture the current setup, we are dealing with a cassandra cluster of 3 nodes and the Eventuate test
application is configured to initialize its keyspace with a `replication-factor` of 3 (you may find further information
on data replication in the
[cassandra
documentation](http://docs.datastax.com/en/cassandra/2.2/cassandra/architecture/archDataDistributeReplication.html)).
As in this example read and write operations are processed on a `QUORUM` consistency level (see
[documentation](http://docs.datastax.com/en/cassandra/2.2/cassandra/dml/dmlConfigConsistency.html)) persisting an
event may only succeed if at least 2 of 3 cassandra nodes are reachable from the Eventuate application.

Please keep in mind that any of the following failure scenarios may take a while until the state between application and
the cassandra cluster has settled to a stable condition (i.e. reconnect timeouts ...).


##### Persisting while one node is down/not reachable

Consequently we expect the application to be able to persist any event while any or no cassandra node is partitioned
from the test application container. You may inspect the state of the application with `docker logs -f location-1` like
mentioned above or trigger a health check via the `./interact.py` python script:

``` bash
# on success the current state counter of the 'ChaosActor' is written to stdout
$ ./interact.py
250

# the check script exits with a non-zero status code on failure
$ echo $?
0
```

The health check of the `interact.py` script instructs the test application to persist a special `HealthCheck` event and
returns with the current counter state if the persist of that given event succeeded within 1 second. The script
basically sends a minor command (`"persist"`) via TCP on port 8080 - so you may achieve the same result via `telnet` as
well.


##### No persistence while a minority of nodes available/reachable

On the contrary the Eventuate application must not be able to persist an event while only a minority of the cassandra
nodes is available to itself. This happens as soon as you partition a combination of any two nodes like:

``` bash
# this command creates a partition of c1 and c3 nodes leaving
# the test application with only one reachable node (c2) behind
$ sudo blockade partition c1,c3
```


##### Reconnect to healthy state

As soon as you remove all existing partitions or give the test application access to at least 2 cassandra nodes the
event persistence should pick up its work again:

``` bash
# remove all existing partitions
$ sudo blockade join
```


##### Examplary test script

Just to give a small example on how you might automate such tests you can find a very simple shell script in
[scenarios/simple-partitions.sh](./scenarios/simple-partitions.sh) that checks for the expectations described above:

``` bash
$ scenarios/simple-partitions.sh
*** checking working persistence with 1 node partition each
partition of cassandra 'c1'
waiting 40 seconds for cluster to settle...
checking health...
partition of cassandra 'c2'
waiting 40 seconds for cluster to settle...
checking health...
partition of cassandra 'c3'
waiting 40 seconds for cluster to settle...
checking health...
*** checking non-working persistence with 2 node partitions
partition of test application + 'c1'
waiting 40 seconds for cluster to settle...
checking health...
waiting till cluster is up and running...
partition of test application + 'c2'
waiting 40 seconds for cluster to settle...
checking health...
waiting till cluster is up and running...
partition of test application + 'c3'
waiting 40 seconds for cluster to settle...
checking health...
waiting till cluster is up and running...
*** checking final reconnect
test successfully finished
```


Additional test scenarios
-------------------------

In the `scenarios` subdirectory of this repository you can find several more complex test scenarios you may inspect
and/or test on your own:

- [counter](./scenarios/counter/): operation based CRDT Counter service using a LevelDB backend
- [counter-cassandra](./scenarios/counter-cassandra/): operation based CRDT Counter service using a Cassandra storage
  backend
- [counter-cassandra-acyclic](./scenarios/counter-cassandra-acyclic/): operation based CRDT Counter service using a
  Cassandra backend with an acyclic replication network
- [state-cassandra](./scenarios/state-cassandra/): Eventuate application that runs a rather simple testing actor which
  accumulates its events (good for testing/debugging purposes)


Auxiliary notes
---------------

Below you can find some random remarks on problems or issues you may run into while chaos testing with the
aforementioned methods and tools.


#### DNS docker image

As mentioned before we are using the docker image `tonistiigi/dnsdock` in order to establish a lightweight DNS service
among the docker containers. This is especially useful when containers are referencing each other in a fashion that
would form a circular dependency of [docker links](http://docs.docker.com/engine/userguide/networking/dockernetworks/).

You may even integrate this DNS docker service into your host's DNS so that you can reference your containers by name.
Usually you just have to add the docker interface's IP to your `/etc/resolv.conf` for that to work.

This docker container can be started with the `start-dns.sh` script and is reachable under the name `dnsdock`. This
means you can observe its current status via

    $ docker logs dnsdock


#### Initial startup time

In case your are using cassandra version `> 2.1` (i.e. `2.2.3` like our example) you have to delay the startup of the
cassandra nodes so that the node topology has enough time to settle in. You may alternatively test with an older version
like `2.1.6` and get rid of those `start_delay` values in your `blockade.yml` resulting in much faster startup times. If
that makes sense depends completely on what you intend to test. The [cassandra
documentation](https://docs.datastax.com/en/cassandra/2.1/cassandra/operations/ops_add_node_to_cluster_t.html) provides
further information on that issue.


#### Interaction with docker commands

While you are using [blockade][blockade] as your 'manager' of your test cluster you are completely free to use other
docker containers in a normal docker-fashion like you are used to. However you **should not** `start`/`stop`/`restart`
containers of your blockade cluster using the usual `docker <cmd>` commands. As [blockade][blockade] has to keep track
of the network devices each container is associated with you should use the respective blockade commands like `blockade
start`, `blockade stop`, `blockade restart`, `blockade up` and `blockade destroy` instead.


[docker]: https://www.docker.com/
[blockade]: https://github.com/kongo2002/blockade
[vagrant]: https://www.vagrantup.com/
[eventuate]: https://github.com/RBMHTechnology/eventuate
[toolbox]: https://docs.docker.com/docker-for-mac/docker-toolbox/
