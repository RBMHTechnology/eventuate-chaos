package com.rbmhtechnology.eventuate.chaos

import akka.actor.ActorRef
import akka.util.Timeout

import com.rbmhtechnology.eventuate.crdt.ORSetService

import scala.concurrent.duration._

class ChaosSetInterface(service: ORSetService[Int]) extends ChaosInterface {
  val setId = "test"

  implicit val timeout = Timeout(1.seconds)

  private def writeSet(set: Set[Int], receiver: ActorRef) = {
    reply(s"[${set.mkString(",")}]", receiver)
  }

  def handleCommand = {
    case ("add", Some(v), recv) =>
      service.add(setId, v).map(x => writeSet(x, recv))
    case ("remove", Some(v), recv) =>
      service.remove(setId, v).map(x => writeSet(x, recv))
    case ("get", None, recv) =>
      service.value(setId).map(x => writeSet(x, recv))
  }
}

