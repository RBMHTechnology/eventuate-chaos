#!/usr/bin/env python

from __future__ import print_function

import argparse
import sys

import crdt_implementations as impl
import interact

HOST = '127.0.0.1'

def get_state(host, port, crdt):
    value = interact.request(host, port, 'get')
    if value:
        return crdt.parse_state(value)
    return None


def check_states(nodes, crdt):
    if len(nodes) < 1:
        raise ValueError('no nodes given')

    all_states = []
    for node, port in nodes.items():
        state = get_state(HOST, port,crdt)
        equal = all(crdt.compare_states(x,state) for x in all_states)

        if not equal:
            print('State of node "%s" [%s] does not match with other states: %s' %
                  (node, crdt.print_state(state), crdt.print_state(all_states[0])))
            return None
        all_states.append(state)

    # all sets are the same -> return the first one for reference
    return all_states[0]


def dump_logs(nodes):
    if len(nodes) < 1:
        raise ValueError('no nodes given')

    for _, port in nodes.items():
        interact.request(HOST, port, 'dump')

def crdt_to_test(argument):
    switcher = {
        "counter": impl.CounterTest(),
        "awset": impl.AWSetTest()
    }
    return switcher.get(argument)


if __name__ == '__main__':
    PARSER = argparse.ArgumentParser(description='start CRDT-set chaos test', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    PARSER.add_argument('--crdt', required=True, choices=['counter', 'awset'], help='crdt implementation to test ')
    PARSER.add_argument('-i', '--iterations', type=int, default=30, help='number of failure/partition iterations')
    PARSER.add_argument('--interval', type=float, default=0.1, help='delay between requests')
    PARSER.add_argument('-l', '--locations', type=int, default=3, help='number of locations')
    PARSER.add_argument('-d', '--delay', type=int, default=10, help='delay between each random partition')
    PARSER.add_argument('--settle', type=int, default=60, help='number of seconds to wait for settling')
    PARSER.add_argument('-r', '--runners', type=int, default=3, help='number of request runners')
    PARSER.add_argument('--restarts', default=0.2, type=float, help='restart probability (in %%)')

    ARGS = PARSER.parse_args()

    crdt = crdt_to_test(ARGS.crdt)

    # we are making some assumptions in here:
    # every location is named 'location<id>' and its TCP port (8080)
    # is mapped to the host port '10000+<id>'
    NODES = dict(('location%d' % idx, 10000+idx) for idx in xrange(1, ARGS.locations+1))
    OPS = [crdt.operation() for _ in xrange(ARGS.runners)] # type of Operation

    interact.wait_to_be_running(HOST, NODES)

    INITIAL_STATE = check_states(NODES,crdt)
    if INITIAL_STATE is None:
        sys.exit(1)

    if not interact.requests_with_chaos(OPS, HOST, NODES, ARGS.iterations, ARGS.interval, ARGS.settle, ARGS.delay, ARGS.restarts):
        sys.exit(1)

    FINAL_STATE = check_states(NODES,crdt)

    if FINAL_STATE is None:
        sys.exit(1)
    else:
        print('All %d nodes converged to the same state: %s' % (len(NODES), crdt.print_state(FINAL_STATE)))


