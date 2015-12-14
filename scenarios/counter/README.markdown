
# Chaos test of distributed operation-based Counter CRDT

This scenario is another example of an [Eventuate][eventuate] application that is being tested under network failures
and partitions.

> In case you haven't read the basic README of this repository already you should definitely do so before continuing
> with this test scenario to gain as much benefit of the following explanations as possible.


## Test setup

This time the application is a [operation-based Counter
CRDT](https://github.com/RBMHTechnology/eventuate/blob/master/src/main/scala/com/rbmhtechnology/eventuate/crdt/Counter.scala)
service that is distributed among three Eventuate locations (each represented by
a separate node). The persistence of the event-sourced services is driven by
[LevelDB event
logs](http://rbmhtechnology.github.io/eventuate/reference/event-log.html#leveldb-storage-backend).


#### Participating nodes

Each Eventuate location accepts control commands on its local TCP port `8080` that is exposed to the host machine on the
following ports:

- `location-1`: TCP port `8080` -> host port `10001`
- `location-2`: TCP port `8080` -> host port `10002`
- `location-3`: TCP port `8080` -> host port `10003`


#### Scenario

The distributed counter CRDT that is managed by the tested application can be incremented and decremented by issuing a
simple command via the TCP port of the respective node.

- `inc 15`: increment the counter by `15`
- `dec 2`: decrement by `2`
- `get`: retrieve the current counter value

Once again you may use the `interact.py` helper script that is shipped with this repository:

``` bash
# start up test scenario
scenarios/counter $ sudo blockade up

# increment counter at location-2
scenarios/counter $ ../../interact.py -p 10002 inc 20
20

# decrement counter at location-3
scenarios/counter $ ../../interact.py -p 10003 dec 4
16

# request counter value from location-1
scenarios/counter $ ../../interact.py -p 10001 get
16
```


#### Scripted chaos test

Just to give you another example on how you might automate your chaos testing you may use the
`crdt-counter-partitions.py` script to generate random network failures while requesting counter operations of the test
application:

``` bash
scenarios/counter $ sudo ../../crdt-counter-partitions.py -i 5 --interval 0.001
Chaos iterations: 5
Request interval: 0.001 sec
Nodes:
  location-1
  location-2
  location-3
Waiting for 3 nodes to be up and running...
Starting requests...
Partition 1: location-1
Partition 2: location-3
Partition 3: location-2
--------------------
Partition 1: location-3, location-1
Partition 2: location-2
--------------------
Partition 1: location-3
Partition 2: location-1
Partition 3: location-2
--------------------
Cluster joined
--------------------
Partition 1: location-3
Partition 2: location-2, location-1
--------------------
Joining cluster - waiting 30 seconds to settle...
Processed 6631 requests in the meantime
Counter value (9037) matches up correctly
```

The expected and tested behavior is to end up with all Eventuate nodes responding with the same counter value after the
network failures have been resolved. Even though during the test run nodes are partitioned from each other the counters
are supposed to converge to the same correct value. Moreover the actual counter value is compared with the value that is
expected by calculating all requests that are made during the test. Of course this value won't match up in case you
issue commands by yourself during the test setup.

The default settings of the test script are to inject random partitions on the network every 10 seconds and moreover
toggling one random node's network to being *slow*, *flaky* or *fast* (more information on [blockade][blockade])
separated by 5 seconds each. These failures are injected on 30 iterations by default. In the meantime every 0.1 seconds
a request for either a *decrement* or *increment* operation is triggered towards one random Eventuate node.

Some parameters like request interval (`--interval`), number of network failures to inject (`--iterations`), the number
of participating Eventuate nodes (`--locations`) and the delay between each failure (`--delay`) may be configured at the
command line as well:

``` bash
$ ./crdt-counter-partitions.py -h
usage: crdt-counter-partitions.py [-h] [-i ITERATIONS] [--interval INTERVAL]
                                  [-l LOCATIONS] [-d DELAY]

start CRDT-counter chaos test

optional arguments:
  -h, --help            show this help message and exit
  -i ITERATIONS, --iterations ITERATIONS
                        number of failure/partition iterations
  --interval INTERVAL   delay between requests
  -l LOCATIONS, --locations LOCATIONS
                        number of locations
  -d DELAY, --delay DELAY
                        delay between each random partition
```

> Please note the example script requires root permissions as it directly hooks into the blockade toolkit which itself
> needs administrative rights to instrument the underlying network.


### Configuration

As usual the configuration is done via a [blockade][blockade] configuration file:

``` yaml
containers:
  location-1:
    image: eventuate-chaos/sbt
    command: ["test:run-nobootcp com.rbmhtechnology.eventuate.chaos.ChaosCounter location-1 location-2.sbt.docker location-3.sbt.docker"]
    ports:
      10001: 8080
    volumes:
      "${PWD}/../..": /app
    environment:
      HOSTNAME: "location-1.sbt.docker"

  location-2:
    image: eventuate-chaos/sbt
    command: ["test:run-nobootcp com.rbmhtechnology.eventuate.chaos.ChaosCounter location-2 location-1.sbt.docker location-3.sbt.docker"]
    ports:
      10002: 8080
    volumes:
      "${PWD}/../..": /app
    environment:
      HOSTNAME: "location-2.sbt.docker"

  location-3:
    image: eventuate-chaos/sbt
    command: ["test:run-nobootcp com.rbmhtechnology.eventuate.chaos.ChaosCounter location-3 location-1.sbt.docker location-2.sbt.docker"]
    ports:
      10003: 8080
    volumes:
      "${PWD}/../..": /app
    environment:
      HOSTNAME: "location-3.sbt.docker"
```

[blockade]: https://github.com/kongo2002/blockade
[eventuate]: https://github.com/RBMHTechnology/eventuate
