#!/usr/bin/env python

from __future__ import print_function

import argparse
import random
import sys

import blockade.cli
import interact


HOST = '127.0.0.1'
MAX_VALUE = 100


def get_counter(host, port):
    value = interact.request(host, port, 'get')
    if value:
        return int(value)
    return None


def check_counters(nodes):
    if len(nodes) < 1:
        raise ValueError('no nodes given')

    counters = []
    for node, port in nodes.items():
        counter = get_counter(HOST, port)
        equal = all(x == counter for x in counters)

        if not equal:
            print('Counter of node "%s" [%s] does not match with other counters: %s' %
                  (node, counter, counters))
            return None
        counters.append(counter)

    # all counters are the same -> return the first one for reference
    return counters[0]


def dump_logs(nodes):
    if len(nodes) < 1:
        raise ValueError('no nodes given')

    for node, port in nodes.items():
        interact.request(HOST, port, 'dump')

    return ''

class CounterOperation(interact.Operation):

    def __init__(self):
        self.state = {'counter': 0}

    def init(self, host, nodes):
        return self.state

    def operation(self, node, idx, state):
        op = random.choice(['inc', 'dec'])
        value = random.randint(1, MAX_VALUE)

        if op == 'inc':
            state['counter'] += value
        else:
            state['counter'] -= value

        return '%s %d' % (op, value)

    def get_diff(self):
        return self.state['counter']


if __name__ == '__main__':
    PARSER = argparse.ArgumentParser(description='start CRDT-counter chaos test')
    PARSER.add_argument('-i', '--iterations', type=int, default=30, help='number of failure/partition iterations')
    PARSER.add_argument('--interval', type=float, default=0.1, help='delay between requests')
    PARSER.add_argument('-l', '--locations', type=int, default=3, help='number of locations')
    PARSER.add_argument('-d', '--delay', type=int, default=10, help='delay between each random partition')
    PARSER.add_argument('--settle', type=int, default=60, help='number of seconds to wait for settling')
    PARSER.add_argument('-r', '--runners', type=int, default=1, help='number of request runners')
    PARSER.add_argument('--restarts', default=0.2, type=float, help='restart probability (in %%)')

    ARGS = PARSER.parse_args()

    # we are making some assumptions in here:
    # every location is named 'location<id>' and its TCP port (8080)
    # is mapped to the host port '10000+<id>'
    NODES = dict(('location%d' % idx, 10000+idx) for idx in xrange(1, ARGS.locations+1))
    OPS = [CounterOperation() for _ in xrange(ARGS.runners)]

    interact.wait_to_be_running(HOST, NODES)

    INITIAL_COUNTER = check_counters(NODES)
    if INITIAL_COUNTER is None:
        sys.exit(1)

    if not interact.requests_with_chaos(OPS, HOST, NODES, ARGS.iterations, ARGS.interval, ARGS.settle, ARGS.delay, ARGS.restarts):
        sys.exit(1)

    DIFF = sum(op.get_diff() for op in OPS)
    COUNTER_VALUE = check_counters(NODES)
    #dump_logs(NODES)

    if COUNTER_VALUE is None:
        sys.exit(1)
    else:
        print('All %d nodes converged to the counter value: %d' % (len(NODES), COUNTER_VALUE))

    EXPECTED_VALUE = INITIAL_COUNTER + DIFF
    if COUNTER_VALUE != EXPECTED_VALUE:
        print('Expected counter value: %d; actual %d' % (EXPECTED_VALUE, COUNTER_VALUE))
        sys.exit(1)

    print('Counter value (%d) matches up correctly' % EXPECTED_VALUE)

