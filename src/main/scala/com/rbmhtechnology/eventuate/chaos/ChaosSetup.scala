package com.rbmhtechnology.eventuate.chaos

import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.BackoffSupervisor
import com.rbmhtechnology.eventuate.ReplicationConnection
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.DurationInt

trait ChaosSetup extends App {

  def getSystem: ActorSystem

  def getEndpoint(implicit system: ActorSystem): ReplicationEndpoint

  protected def baseConfig(hostname: String) = ConfigFactory.parseString(
    s"""
       |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
       |akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
       |akka.remote.netty.tcp.hostname = "$hostname"
       |akka.remote.netty.tcp.port = 2552
       |akka.test.single-expect-default = 10s
       |akka.loglevel = "INFO"
       |eventuate.log.write-batch-size = 16
       |eventuate.log.read-timeout = 3s
       |eventuate.log.retry-delay = 3s
       |akka.remote.netty.tcp.maximum-frame-size = 1024000b
     """.stripMargin)

  protected def quote(str: String) = "\"" + str + "\""

  /** starts the actor watched by a `BackoffSupervisor` */
  protected def supervised(props: Props, name: String): Props =
    BackoffSupervisor.props(props, name, 1.second, 30.seconds, 0.1)

  def name = {
    if (args == null || args.length < 1) {
      Console.err.println("no <nodename> specified")
      sys.exit(1)
    } else {
      args(0)
    }
  }

  def hostname = sys.env.getOrElse("HOSTNAME", s"$name.eventuate-chaos.docker")

  // replication connection to other node(s)
  def connections = args.drop(1).map { conn =>
    conn.split(":") match {
      case Array(host, port) =>
        ReplicationConnection(host, port.toInt)
      case Array(host) =>
        ReplicationConnection(host, 2552)
    }
  }.toSet
}
