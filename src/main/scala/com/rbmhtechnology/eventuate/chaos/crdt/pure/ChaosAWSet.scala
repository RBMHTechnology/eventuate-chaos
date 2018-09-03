package com.rbmhtechnology.eventuate.chaos.crdt.pure

import akka.actor.ActorRef
import akka.actor.Props
import akka.util.Timeout
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.chaos.ChaosInterface
import com.rbmhtechnology.eventuate.chaos.ChaosLeveldbSetup
import com.rbmhtechnology.eventuate.crdt.pure.AWSetService

class ChaosAWSetInterface(service: AWSetService[Int]) extends ChaosInterface {

  val setId = "test"

  import scala.concurrent.duration._
  implicit val timeout = Timeout(1.seconds)

  private def writeSet(set: Set[Int], receiver: ActorRef) = {
    reply(s"[${set.mkString(",")}]", receiver)
  }

  def handleCommand = {
    case ("add", Some(v), recv) =>
      service.add(setId, v).map(x => writeSet(x, recv))
    case ("remove", Some(v), recv) =>
      service.remove(setId, v).map(x => writeSet(x, recv))
    case ("clear", None, recv) =>
      service.clear(setId).map(x => writeSet(x, recv))
    case ("get", None, recv) =>
      service.value(setId).map(x => writeSet(x, recv))
  }

}

object ChaosAWSetLeveldb extends ChaosLeveldbSetup {
  implicit val system = getSystem
  val endpoint = getEndpoint

  val service = new AWSetService[Int](name, endpoint.logs(ReplicationEndpoint.DefaultLogName))
  system.actorOf(Props(new ChaosAWSetInterface(service)))
}

