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

import java.net.InetAddress

import scala.collection.immutable.Seq
import scala.sys.process._
import scala.util.Try

trait ChaosCommands {
  private lazy val environment: Array[(String, String)] =
    Seq("boot2docker", "shellinit").lineStream
      .map(_.trim)
      .filter(_.startsWith("export "))
      .map(_.substring(7))
      .map(_.split("="))
      .foldLeft(Seq[(String, String)]()) { case (acc, kv) => acc :+ (kv(0) -> kv(1)) }.toArray

  def startCluster(numNodes: Int): Try[Unit] =
    runCommand(Seq("./cluster-start.sh", numNodes.toString))

  def stopCluster(): Try[Unit] =
    runCommand("./cluster-stop.sh")

  def startNodes(nodes: Seq[Int]): Try[Unit] =
    runCommand(Seq("docker", "start") ++ nodes.map(i => s"cassandra-$i"))

  def killNodes(nodes: Seq[Int]): Try[Unit] =
    runCommand(Seq("docker", "kill") ++ nodes.map(i => s"cassandra-$i"))

  def seedAddress(): Try[InetAddress] =
    runCommand(Seq("docker", "inspect", "--format='{{ .NetworkSettings.IPAddress }}'", "cassandra-1"), _.!!.trim).map(InetAddress.getByName(_))

  private def runCommand(command: String): Try[Unit] =
    runCommand(Seq(command))

  private def runCommand(command: Seq[String]): Try[Unit] =
    runCommand(command, _.lineStream.foreach(println))

  private def runCommand[A](command: Seq[String], f: ProcessBuilder => A): Try[A] =
    Try(f(Process(command, None, environment: _*)))

}
