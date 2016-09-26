#!/usr/bin/env python

from __future__ import print_function

import argparse
import socket
import sys
import time
import threading
import random

import blockade.cli


BUFFER_SIZE = 64
SETTLE_TIMEOUT = 60
FAILURE_DELAY = 5


class Operation(object):
    '''Abstract base class that is used for "RequestWorker" operations
    '''
    def init(self, host, nodes):
        pass

    def operation(self, node, iteration):
        '''
        This method will be called on very iteration of a worker request.
        '''
        raise NotImplementedError("you have to implement 'Operation.operation'")


class RequestWorker(threading.Thread):
    def __init__(self, host, nodes, operation, operations=None, interval=0.5):
        threading.Thread.__init__(self)

        self.host = host
        self.nodes = nodes
        self.operations = operations
        self.interval = interval
        self.is_cancelled = False
        self.operation = operation
        self.iterations = 0

    def run(self):
        # initialize operation's state (if necessary)
        self.operation.init(self.host, self.nodes)

        while (self.operations is None or self.iterations < self.operations) and not self.is_cancelled:
            node = random.choice(self.nodes.keys())
            port = self.nodes[node]
            request(self.host, port, self.operation.operation(node, self.iterations))

            self.iterations += 1
            time.sleep(self.interval)

    def cancel(self):
        self.is_cancelled = True


def request(ip, port, message):
    try:
        # connect
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(1.0)
        sock.connect((ip, port))

        try:
            # request
            sock.send(message)
            data = []

            while True:
                received = sock.recv(BUFFER_SIZE)
                if not received:
                    break
                data.append(received)
        except socket.timeout:
            pass
        finally:
            # disconnect
            sock.close()
        return ''.join(data)
    except socket.error:
        return None


def is_healthy(ip, port, message, verbose=True):
    try:
        data = request(ip, port, message)
    except socket.error:
        return False

    if data and len(data) > 0:
        if verbose:
            print(data)
        return True
    return False


def wait_to_be_running(host, nodes):
    print('Waiting for %d nodes to be up and running...' % (len(nodes)))
    while True:
        all_running = all(is_healthy(host, port, 'get', False) for port in nodes.values())
        if all_running:
            return True
        time.sleep(2)


def _print_partitions(partitions):
    if len(partitions) < 1:
        print('Cluster joined')
    else:
        for idx, part in enumerate(partitions):
            print('Partition %d: %s' % (idx+1, ', '.join(part)))


def requests_with_chaos(operations, host, nodes, iterations, interval, settle=SETTLE_TIMEOUT, failure_delay=FAILURE_DELAY, restarts=0.2):
    print('Chaos iterations: %d' % iterations)
    print('Request interval: %.3f sec' % interval)
    print('Operation runners: %d' % len(operations))
    print('Failure delay: %d sec' % failure_delay)

    restart_prob = max(min(restarts, 1.0), 0.0)
    print('Restart probability: %.2f%%' % (restart_prob * 100.0))

    print('Nodes:')
    for node in nodes.keys():
        print('  ' + node)

    # wait for system to be ready and initialized
    wait_to_be_running(host, nodes)

    workers = [RequestWorker(host, nodes, op, interval=interval) for op in operations]

    def for_workers(func):
        for worker in workers:
            func(worker)
    try:
        for_workers(RequestWorker.start)

        # initialize blockade interface
        cfg = blockade.cli.load_config('blockade.yml')
        blk = blockade.cli.get_blockade(cfg)

        def random_network():
            failure = random.choice([blk.fast, blk.flaky, blk.slow])
            return failure(None, None, select_random=True)

        delay = failure_delay / 2

        for _ in xrange(iterations):
            # trigger some random partition(s)
            part = blk.random_partition()
            _print_partitions(part)
            print('-' * 25)
            time.sleep(delay)

            # we either schedule a restart of a
            # running node or we trigger a network failure
            if random.random() < restart_prob:
                restarted = list(blk.restart(None, blk.state_factory.load(), select_random=True))
                print('Restarting %s...' % (', '.join(restarted)))
                print('-' * 25)
            else:
                # random network failure (slow, flaky...)
                random_network()

            time.sleep(delay)

    except (KeyboardInterrupt, blockade.errors.BlockadeError) as err:
        for_workers(RequestWorker.cancel)
        for_workers(RequestWorker.join)

        if err is KeyboardInterrupt:
            blk.join()
            blk.fast(None, None)
            print('Test interrupted')
            return False
        else:
            raise

    for_workers(RequestWorker.cancel)
    for_workers(RequestWorker.join)

    print('Joining cluster - waiting %d seconds to settle...' % settle)
    blk.join()
    blk.fast(None, None)

    time.sleep(settle)

    print('Processed %d requests in the meantime' % sum(w.iterations for w in workers))
    return True


if __name__ == '__main__':
    PARSER = argparse.ArgumentParser(description='Simple interaction with a TCP endpoint')
    PARSER.add_argument('-p', '--port', type=int, default=8080, help='port of the remote endpoint')
    PARSER.add_argument('-i', '--ip', default='127.0.0.1', help='IP of the remote endpoint')
    PARSER.add_argument('message', nargs='*', default=['persist'])

    ARGS = PARSER.parse_args()
    MESSAGE = ' '.join(ARGS.message)

    if not is_healthy(ARGS.ip, ARGS.port, MESSAGE):
        sys.exit(1)
