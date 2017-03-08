package com.rbmhtechnology.eventuate.chaos

import akka.actor.ActorSystem
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.log.cassandra.CassandraEventLog
import com.typesafe.config.ConfigFactory

trait ChaosCassandraSetup extends ChaosSetup {

  def config(hostname: String, seeds: Seq[String]) = baseConfig(hostname)
    .withFallback(ConfigFactory.parseString(
    s"""
       |eventuate.log.cassandra.contact-points = [${seeds.map(quote).mkString(",")}]
       |eventuate.log.cassandra.replication-factor = ${seeds.size}
       |eventuate.log.cassandra.keyspace = "ev_$name"
       |eventuate.log.cassandra.index-update-limit = 16
       |eventuate.log.cassandra.read-consistency = "QUORUM"
       |eventuate.log.cassandra.write-consistency = "QUORUM"
       |eventuate.log.cassandra.init-retry-max = 50
       |eventuate.log.cassandra.connect-retry-max = 50
     """.stripMargin))

  def cassandras = sys.env.get("CASSANDRA_NODES").map(_.split(","))
    .getOrElse(Array("c1.cassandra.docker", "c2.cassandra.docker"))

  def getSystem = ActorSystem.create("location", config(hostname, cassandras))

  // create and activate eventuate replication endpoint
  def getEndpoint(implicit system: ActorSystem) = {
    val ep = new ReplicationEndpoint(name,
      Set(ReplicationEndpoint.DefaultLogName),
      CassandraEventLog.props(_),
      connections)
    ep.activate()
    ep
  }
}
