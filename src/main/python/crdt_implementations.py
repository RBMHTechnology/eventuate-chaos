
import random
import interact

MAX_VALUE = 100

### Operations

class AWSetOperation(interact.Operation):

    def operation(self, node, iteration):
        op = random.choice(['add', 'remove','clear'])
        value = random.randint(1, MAX_VALUE)

        return '%s %d' % (op, value)

class CounterOperation(interact.Operation):

    def __init__(self):
        self.counter = 0

    def operation(self, node, idx):
        op = random.choice(['inc', 'dec'])
        value = random.randint(1, MAX_VALUE)

        if op == 'inc':
            self.counter += value
        else:
            self.counter -= value

        return '%s %d' % (op, value)

    def get_counter(self):
        return self.counter

### Tests

class CRDTTest(object):
    def operation(self, node, iteration):
        raise NotImplementedError("you have to implement 'operation'")

    def compare_states(self,state1,state2):
        raise NotImplementedError("you have to implement 'compare_states'")

    def parse_state(self,state):
        raise NotImplementedError("you have to implement 'parse_state'")

    def print_state(self,state):
        raise NotImplementedError("you have to implement 'print_state'")

class AWSetTest(CRDTTest):
    def operation(self):
        return AWSetOperation()

    def compare_states(self,awset1,awset2):
        return len(awset1.difference(awset2)) == 0 & len(awset2.difference(awset1)) == 0

    def parse_state(self,state):
        return set(state[1:-1].split(","))

    def print_state(self,state):
        return repr(state)

class CounterTest(CRDTTest):
    def operation(self):
        return CounterOperation()

    def compare_states(self,counter1,counter2):
        return counter1 == counter2

    def parse_state(self,state):
        return int(state)

    def print_state(self,state):
        return str(state)

