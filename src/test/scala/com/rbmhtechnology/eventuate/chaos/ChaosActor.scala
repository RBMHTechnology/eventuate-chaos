/*
 * Copyright (C) 2015 Red Bull Media House GmbH <http://www.redbullmediahouse.com> - all rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rbmhtechnology.eventuate.chaos

import akka.actor._

import com.rbmhtechnology.eventuate._
import com.rbmhtechnology.eventuate.chaos.ChaosActorInterface.HealthCheckResult
import com.rbmhtechnology.eventuate.log.cassandra._
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.io.StdIn
import scala.util._
import scala.concurrent.duration._

object ChaosActor extends App with ChaosCommands {
  def defaultConfig(seeds: Seq[String]) = ConfigFactory.parseString(
    s"""
       |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
       |akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
       |akka.remote.netty.tcp.hostname = "127.0.0.1"
       |akka.remote.netty.tcp.port = 2552
       |akka.test.single-expect-default = 10s
       |akka.loglevel = "ERROR"
       |
       |eventuate.log.cassandra.contact-points = [${seeds.map(quote).mkString(",")}]
       |eventuate.log.cassandra.replication-factor = 3
     """.stripMargin)

  private def quote(str: String) = "\"" + str + "\""

  def runChaosActor(seeds: String*): Unit = {
    val system = ActorSystem("chaos", defaultConfig(seeds))
    val log = system.actorOf(CassandraEventLog.props("chaos"))
    val actor = system.actorOf(Props(new ChaosActor(log)))

    val interface = system.actorOf(Props(new ChaosActorInterface(actor)))

    // -d = daemon mode that does not listen on stdin (i.e. docker instances)
    if (args.contains("-d")) {
      Await.result(system.whenTerminated, Duration.Inf)
    } else {
      StdIn.readLine()
      system.stop(interface)
      system.stop(actor)
      system.terminate
    }
  }

  sys.env.get("CASSANDRA_NODES") match {
    case Some(nodes) if nodes.nonEmpty =>
      runChaosActor(nodes.split(","): _*)
    case _ =>
      seedAddress() match {
        case Failure(err)  => err.printStackTrace()
        case Success(seed) => runChaosActor(seed.getHostName)
      }
  }
}

class ChaosActor(val eventLog: ActorRef) extends EventsourcedActor {
  val id = "chaos"

  // persistent state
  var state: Int = 0

  // transient state
  var failures: Int = 0

  override def onCommand: Receive = {
    case i: Int => persist(i) {
      case Success(i) =>
        scheduleCommand()
      case Failure(e) =>
        failures += 1
        println(s"persist failure $failures: ${e.getMessage}")
        scheduleCommand()
    }

    case cmd: ChaosActorInterface.HealthCheck =>
      val chaosInterface = sender
      persist(cmd) {
        case Success(command) =>
          chaosInterface ! HealthCheckResult(state, command.requester)
        case Failure(e) =>
          println(s"health check persist failed: ${e.getMessage}")
      }
  }

  override def onEvent: Receive = {
    case i: Int =>
      state += i
      println(s"counter = $state (recovery = $recovering)")
  }

  import context.dispatcher

  override def preStart(): Unit = {
    super.preStart()
    scheduleCommand()
  }

  override def postStop(): Unit = {
    schedule.foreach(_.cancel())
    super.postStop()
  }

  private def scheduleCommand(): Unit =
    schedule = Some(context.system.scheduler.scheduleOnce(2.seconds, self, 1))

  private var schedule: Option[Cancellable] = None
}
