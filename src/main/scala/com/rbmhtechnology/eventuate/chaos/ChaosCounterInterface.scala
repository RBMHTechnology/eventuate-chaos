package com.rbmhtechnology.eventuate.chaos

import com.rbmhtechnology.eventuate.crdt.CounterService

class ChaosCounterInterface(service: CounterService[Int]) extends ChaosInterface {
  val testId = "test"

  def handleCommand = {
    case ("inc", Some(v), recv) =>
      service.update(testId, v)
        .map(value => reply(value.toString, recv))
        .onFailure {
          case err => log.error(err, "Failed to increment counter by {}", v)
        }
    case ("dec", Some(v), recv) =>
      service.update(testId, -v)
        .map(value => reply(value.toString, recv))
        .onFailure {
          case err => log.error(err, "Failed to decrement counter by {}", v)
        }
    case ("get", None, recv) =>
      println("##################### GET Counter")
      service.value(testId)
        .map(value => reply(value.toString, recv))
        .onFailure {
          case err => log.error(err, "Failed to retrieve counter value")
        }
  }
}
