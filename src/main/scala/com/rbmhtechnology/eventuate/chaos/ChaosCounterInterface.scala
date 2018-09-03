package com.rbmhtechnology.eventuate.chaos

import com.rbmhtechnology.eventuate.chaos.ChaosCounterInterface.ChaosCounterService
import com.rbmhtechnology.eventuate.crdt._

import scala.concurrent.Future


object ChaosCounterInterface {

  def apply(service: CounterService[Int]): ChaosCounterInterface = new ChaosCounterInterface(plainCounterService(service))

  def apply(service: pure.CounterService[Int]): ChaosCounterInterface = new ChaosCounterInterface(pureCounterService(service))

  trait ChaosCounterService[A] {
    def update(id: String, delta: A): Future[A]

    def value(id: String): Future[A]
  }
  def plainCounterService[A](service: CounterService[A]): ChaosCounterService[A] = new ChaosCounterService[A] {
    override def update(id: String, delta: A): Future[A] = service.update(id, delta)
    override def value(id: String): Future[A] = service.value(id)
  }
  def pureCounterService[A](service: pure.CounterService[A]): ChaosCounterService[A] = new ChaosCounterService[A] {
    override def update(id: String, delta: A): Future[A] = service.update(id, delta)
    override def value(id: String): Future[A] = service.value(id)
  }
}

class ChaosCounterInterface(service: ChaosCounterService[Int]) extends ChaosInterface {

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
