#!/usr/bin/env python

from __future__ import print_function

import argparse
import random
import sys

import blockade.cli
import interact


HOST = '127.0.0.1'
MAX_VALUE = 100


def check_counters(nodes):
    if len(nodes) < 1:
        raise ValueError('no nodes given')

    counters = []
    for node, port in nodes.items():
        counter = interact.request(HOST, port, 'get')
        equal = all(x == counter for x in counters)

        if not equal:
            print('Counter of node "%s" [%s] does not match with other counters: %s' %
                  (node, counter, str(counters)))
            return None
        counters.append(counter)

    # all counters are the same -> return the first one for reference
    return int(counters[0])


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
        # get first counter value
        counter = int(interact.request(host, nodes.values()[0], 'get'))
        self.state['counter'] = counter

        return self.state

    def operation(self, node, idx, state):
        op = random.choice(['inc', 'dec'])
        value = random.randint(1, MAX_VALUE)

        if op == 'inc':
            state['counter'] += value
        else:
            state['counter'] -= value

        return '%s %d' % (op, value)

    def get_counter(self):
        return self.state['counter']


if __name__ == '__main__':
    SETTLE_TIMEOUT = 120
    PARSER = argparse.ArgumentParser(description='start CRDT-counter chaos test')
    PARSER.add_argument('-i', '--iterations', type=int, default=30, help='number of failure/partition iterations')
    PARSER.add_argument('--interval', type=float, default=0.1, help='delay between requests')
    PARSER.add_argument('-l', '--locations', type=int, default=3, help='number of locations')
    PARSER.add_argument('-d', '--delay', type=int, default=10, help='delay between each random partition')

    ARGS = PARSER.parse_args()

    # we are making some assumptions in here:
    # every location is named 'location<id>' and its TCP port (8080)
    # is mapped to the host port '10000+<id>'
    NODES = dict(('location%d' % idx, 10000+idx) for idx in xrange(1, ARGS.locations+1))
    OP = CounterOperation()

    if not interact.requests_with_chaos(OP, HOST, NODES, ARGS.iterations, ARGS.interval, SETTLE_TIMEOUT, ARGS.delay):
        sys.exit(1)

    EXPECTED_VALUE = OP.get_counter()
    COUNTER_VALUE = check_counters(NODES)

    if COUNTER_VALUE is None:
        sys.exit(1)
    else:
        print('All %d nodes converged to the counter value: %d' % (len(NODES), COUNTER_VALUE))

    if COUNTER_VALUE != EXPECTED_VALUE:
        print('Expected counter value: %d; actual %d' % (EXPECTED_VALUE, COUNTER_VALUE))
        sys.exit(1)

    print('Counter value (%d) matches up correctly' % EXPECTED_VALUE)

