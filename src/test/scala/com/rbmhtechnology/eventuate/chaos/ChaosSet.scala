package com.rbmhtechnology.eventuate.chaos

import akka.actor.Props
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.crdt.ORSetService

object ChaosSet extends ChaosCassandraSetup {
  implicit val system = getSystem
  val endpoint = getEndpoint

  val service = new ORSetService[Int](name, endpoint.logs(ReplicationEndpoint.DefaultLogName))
  system.actorOf(Props(new ChaosSetInterface(service)))
}

