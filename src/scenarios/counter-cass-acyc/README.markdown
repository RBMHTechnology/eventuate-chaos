
# Chaos test of distributed operation-based Counter CRDT

This test configuration establishes a setup of three eventuate locations connecting with a three node Cassandra
(version `2.2.3`) cluster. The eventuate locations are connected in an acyclic (linear) replication network.

- Eventuate locations
    - `location1`
    - `location2`
    - `location3`
- Cassandra nodes
    - `c1` (seed node)
    - `c11`
    - `c12`

## Quick start

```
sudo blockade up
sudo ../../crdt-counter-partitions.py
```

## Help

Please consult the related [README](./../counter/) of the *Counter CRDT* example for further information on these
advanced test setups.
