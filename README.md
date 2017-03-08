Eventuate chaos testing utilities
=================================

This repository contains some chaos testing utilities for [Eventuate][eventuate], [Apache
Cassandra](http://cassandra.apache.org/) and [Level-DB](https://github.com/google/leveldb). They support running
Cassandra clusters and Eventuate applications with [Docker][docker] and using [blockade][blockade] to easily generate
failures like stopping and restarting of containers and introducing network failures such as partitions, packet loss and
slow connections.

This repository can be seen as a toolkit or collection of utilities which you can use to test your Eventuate
applications. An exemplary test setup gives an introduction into these tools and
serves as a blueprint to build your own more complex test scenarios.

In addition to this the repository contains five test scenarios that are used for testing Eventuate itself. One of them is 
invoked through a [travis build](https://travis-ci.org/RBMHTechnology/eventuate-chaos) so the corresponding [`.travis.yml`](.travis.yml) 
also serves as a blueprint on how to setup travis for continuous chaos tests.


##### Blockade

The test scenarios facilitate the [blockade][blockade] tool. 
Blockade is a utility for
testing network failures and partitions in distributed applications. Currently the tests are based 
on a fork of the original [dcm-oss/blockade](https://github.com/dcm-oss/blockade). The next release 
of that would allow to switch back to the original. Blockade uses Docker containers to run
application processes and manages the network from the host system to create various failure scenarios. Docker itself is
a container engine that allows you to package and run applications in a hardware-agnostic and platform-agnostic fashion -
often called like somewhat *lightweight virtual machines*.

You can find further information on the [blockade github page][blockade] regarding its configuration and the
possibilities you have in addition to what is mentioned in here.


Prerequisites
-------------

### Using the vagrant VM

The [Vagrantfile](Vagrantfile) can be used to setup a virtual box VM that is ready ro tun the test-scenarios. It requires:

- [Vagrant][vagrant] (tested with 1.7.4)
- [VirtualBox](https://www.virtualbox.org/)

To setup the (or start an existing) VM simply run
```bash
$ vagrant up
```

The VM is based on an ubuntu/trusty64 image with docker installed. The custom 
[provision](provision.sh) script for the VM:

- Clones the blockade fork into `/blockade` and installs it in the pyhton environment
- downloads docker images for cassandra 2.x/3.x and [dnsdock][dnsdock]
- builds the docker image for the eventuate chaos test application. The image expects that the 
  eventuate-chaos root folder is mounted under `/app` and the application is pre-packaged in `/app/target/universal/stage`
   
### Using Native Linux

To run the test scenarios natively under Linux the following is required:

1. [Docker][docker] (tested with docker >= 1.6) 
1. the [blockade][blockade] fork
1. docker images for cassandra 2.x/3.x and [dnsdock][dnsdock]
1. docker image for the eventuate-chaos application

Steps 2.-4. can simply be executed through the [`provision.sh`](provision.sh) script:
```bash
$ sudo provision.sh
```

Running the tests
-----------------

In the `src/scenarios` subdirectory of this repository you can find several more complex test scenarios you can try out:

- [counter](./src/scenarios/counter/): operation based CRDT Counter service using a LevelDB backend
- [counter-cassandra](./src/scenarios/counter-cassandra/): operation based CRDT Counter service using a Cassandra 2.x storage
  backend
- [counter-cass-acyc](./src/scenarios/counter-cass-acyc/): operation based CRDT Counter service using a
  Cassandra 2.x storage backend with an acyclic replication network
- [counter-cass3](./src/scenarios/counter-cass3/): operation based CRDT Counter service using a
  Cassandra 3.x storage backend 

Before a test is started the application has to be built through `sbt universal:stage`. This does not only 
compile the application but also pre-packages it under `target/universal/stage` along with all its dependencies 
so it can be run from this folder. When running natively under Linux or when logged into the 
VM through `vagrant ssh`, each test scenario can be started with

```bash
$ src/main/shell/start-test.sh src/scenario/<name>
```

When using the vagrant VM the tests can also be started directly from the host (without logging into the VM) with

```bash
$ src/main/shell/va-start-test.sh src/scenario/<name>
```

In each case a `dockdns` container is started and its address is added to `/etc/resolv.conf` if not already done. 
Afterwards the application containers are started through blockade and subsequently the test script `crdt-counter-partitions.py`.
This script issues commands to increment/decrement the application's counter and concurrently creates _chaos_ through blockade. 
At the end it verifies that all application nodes converge to the same counter value and exits with return code 1
if this is not the case.

`start-test.sh` (likewise `va-start-test.sh`) forwards all arguments to `crdt-counter-partitions.py` so it can be called with `-h` to checkout 
the various parameters for controlling the test run and the _chaos_.


Example test setup
------------------

The examplary test setup we are describing in the following consists of 2 basic components:

- **Eventuate test appliation**:

    This is the Eventuate application we are actually testing with. It is an
    [EventsourcedActor](http://rbmhtechnology.github.io/eventuate/user-guide.html#event-sourced-actors) that continually
    (every 2 seconds) emits an event that increments an internal counter by persisting it changes. You can find its
    implementation in [ChaosActor.scala](./src/test/scala/com/rbmhtechnology/eventuate/chaos/ChaosActor.scala). The
    container running the scala application is named `location-1`.

- **Cassandra cluster**:

    This cassandra cluster contains 3 nodes each running in its own Docker container. The seed node `c1` is started 
    initially while the remaining nodes `c2` and `c3` join the cluster later on.

The Eventuate test application has to be built and pre-packaged through `sbt universal:stage`.

### DNS docker image

As mentioned before we are using the docker image `tonistiigi/dnsdock` in order to establish a lightweight DNS service
among the docker containers. This is especially useful when containers are referencing each other in a fashion that
would form a circular dependency of [docker links](http://docs.docker.com/engine/userguide/networking/dockernetworks/).

This docker container can be started with the `start-dns.sh` script and is reachable under the name `dnsdock`. 
The script `start-dns.sh` will look for the default `docker0` network interface and start a `dnsdock` image on port 53 for DNS
discovery. You may have to specify the location to your *docker socket* via `$DOCKER_SOCKET` in case the script does not
find it by itself:

```bash
$ DOCKER_SOCKET=/var/run/docker.sock ./src/main/shell/start-dns.sh
```

The current status of the dns service can be observed via

```bash
$ docker logs dnsdock
```

You may even integrate this DNS docker service into your host's DNS so that you can reference your containers by name.
Usually you just have to add the docker interface's IP to your `/etc/resolv.conf` for that to work. The script 
will do this for you, if you run it as root with the command line option `-f`

```bash
$ sudo ./src/main/shell/start-dns.sh -f
```

### Configuration

The test setup described above is encoded in the `blockade.yml` YAML configuration file that is used by
[blockade][blockade] to manage the test cluster:

```yaml
containers:
  c1:
    image: cassandra:2.2.3

  c2:
    image: cassandra:2.2.3
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
    image: eventuate-chaos
    command: ["-main",  "com.rbmhtechnology.eventuate.chaos.ChaosActor",  "-d"]
    volumes:
      "${PWD}": "/app"
    links: [ "c2", "c3" ]
    environment:
      CASSANDRA_NODES: "c1.cassandra.docker,c2.cassandra.docker,c3.cassandra.docker"
```

The configuration shows the three cassandra nodes (`c1`, `c2`, `c3`) and the application node (`location-1`) 
each one running in its container. The `links` from `c2` and `c3` to `c1` just exist to ensure that 
`c1` is started first and the others can connect to it (`CASSANDRA_SEEDS: "c1.cassandra.docker"`). 
The names `<container-name>.<image-name>.docker` are resolved by the DNS service started through `start-dns.sh`.

Depending on the cassandra version (> 2.1) you have to configure a reasonable startup delay for each cassandra node
that joins the cluster. The first (here `c2`) can join immediately, each following node needs a `startup-delay` 
that increases linearly (here `c3` waits 60 seconds and thus a `c4` would need a delay of 120 seconds) (See the 
[cassandra documentation](https://docs.datastax.com/en/cassandra/2.1/cassandra/operations/ops_add_node_to_cluster_t.html) for
further information). 

The application (`com.rbmhtechnology.eventuate.chaos.ChaosActor`) is started directly from the build directory 
`target/universal/stage`. That is why eventuate-chaos' root folder is mounted (as volume) under 
`/app` in the application's container (`$PWD` assumes that `blockade.yml` is in the project's root folder).


Testing the setup
-----------------

### blockade chaos cluster

Now that you have all prerequisites fulfilled you can start up the [blockade][blockade] test cluster with a 
`blockade.yml` as given above. From this point on you may freely experiment and modify all given
parameters and configurations as this is just a toolkit to get you started as quickly as possible.

> Note that all blockade commands require root permissions.

```bash
$ sudo blockade up
```

Once all nodes are up and running you can
inspect your running cluster via `blockade status`:

```bash
$ sudo blockade status
NODE            CONTAINER ID    STATUS  IP              NETWORK    PARTITION
c1              8ccf90e8f76e    UP      172.17.0.3      NORMAL
c2              5c65f3ca62ec    UP      172.17.0.5      NORMAL
c3              4b630db6a19e    UP      172.17.0.4      NORMAL
location-1      bb8c89f615ef    UP      172.17.0.6      NORMAL
```

### failures

From now on you may introduce any kind of failure the [blockade][blockade] tool supports.

##### partition one cassandra node

```bash
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

```bash
$ sudo blockade partition c1 c3
NODE            CONTAINER ID    STATUS  IP              NETWORK    PARTITION
c1              8ccf90e8f76e    UP      172.17.0.3      NORMAL     1
c2              5c65f3ca62ec    UP      172.17.0.5      NORMAL     3
c3              4b630db6a19e    UP      172.17.0.4      NORMAL     2
location-1      bb8c89f615ef    UP      172.17.0.6      NORMAL     3
```


##### flaky network connection of the Eventuate node

```bash
$ sudo blockade flaky location-1
NODE            CONTAINER ID    STATUS  IP              NETWORK    PARTITION
c1              8ccf90e8f76e    UP      172.17.0.3      NORMAL     1
c2              5c65f3ca62ec    UP      172.17.0.5      NORMAL     3
c3              4b630db6a19e    UP      172.17.0.4      NORMAL     2
location-1      bb8c89f615ef    UP      172.17.0.6      FLAKY      3
```

##### restart of nodes

You may also restart one or multiple nodes and inspect the effect on the Eventuate application as well:

```bash
$ sudo blockade restart c2 c3
```

#### Inspect test application output

While playing with the conditions of the test cluster you can see the Eventuate application output its current state
(being the actual counter value) on stdout. For ongoing inspection run 
`docker logs -f location-1`:


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
mentioned above or trigger a health check via the `./src/main/pyhton/interact.py` python script:

```bash
# on success the current state counter of the 'ChaosActor' is written to stdout
$ ./src/main/pyhton/interact.py
250

# the check script exits with a non-zero status code on failure
$ echo $?
0
```

The health check of the `interact.py` script instructs the test application to persist a special `HealthCheck` event and
returns with the current counter state if the persist of that event succeeded within 1 second. The script
basically sends a minor command (`"persist"`) via TCP on port 8080 - so you may achieve the same result via `telnet` as
well.


##### No persistence while a minority of nodes available/reachable

On the contrary the Eventuate application must not be able to persist an event while only a minority of the cassandra
nodes is available to itself. This happens as soon as you partition a combination of any two nodes like:

```bash
# this command creates a partition of c1 and c3 nodes leaving
# the test application with only one reachable node (c2) behind
$ sudo blockade partition c1,c3
```


##### Reconnect to healthy state

As soon as you remove all existing partitions or give the test application access to at least 2 cassandra nodes the
event persistence should pick up its work again:

```bash
# remove all existing partitions
$ sudo blockade join
```


##### Exemplary test script

Just to give a small example on how you might automate such tests you can find a very simple shell script in
[src/scenarios/simple-partitions.sh](./src/scenarios/simple-partitions.sh) that checks for the expectations described above:

```bash
$ src/scenarios/simple-partitions.sh
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



Auxiliary notes
---------------

Below you can find some random remarks on problems or issues you may run into while chaos testing with the
aforementioned methods and tools.


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
[dnsdock]: https://github.com/aacebedo/dnsdock
