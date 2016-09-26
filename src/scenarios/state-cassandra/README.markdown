
# Chaos test of test actor with accumulated event state

This test configuration establishes a setup of three eventuate locations connecting with a three node Cassandra
(version `2.2.3`) cluster.

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
sudo ../../state-partitions.py
```

## Help

Please consult the related [README](./../counter/) of the *Counter CRDT* example for further information on these
advanced test setups.
