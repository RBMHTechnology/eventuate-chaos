package com.rbmhtechnology.eventuate.chaos

import akka.actor.Props
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.crdt._

object ChaosCounterLeveldb extends ChaosLeveldbSetup {
  implicit val system = getSystem
  val endpoint = getEndpoint

  val service = new CounterService[Int](name, endpoint.logs(ReplicationEndpoint.DefaultLogName))
  system.actorOf(Props(ChaosCounterInterface(service)))
}

object ChaosCounterCassandra extends ChaosCassandraSetup {
  implicit val system = getSystem
  val endpoint = getEndpoint

  val service = new CounterService[Int](name, endpoint.logs(ReplicationEndpoint.DefaultLogName))
  system.actorOf(Props(ChaosCounterInterface(service)))
}

object ChaosPureCounterLeveldb extends ChaosLeveldbSetup {
  implicit val system = getSystem
  val endpoint = getEndpoint

  val service = new pure.CounterService[Int](name, endpoint.logs(ReplicationEndpoint.DefaultLogName))
  system.actorOf(Props(ChaosCounterInterface(service)))
}
