package com.rbmhtechnology.eventuate.chaos

import java.net.InetSocketAddress

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.io.IO
import akka.io.Tcp
import akka.util.ByteString

abstract class ChaosInterface extends Actor with ActorLogging {
  val port = 8080
  val endpoint = new InetSocketAddress(port)
  val command = """(?s)(\w+)\s+(\d+).*""".r

  implicit val ec = context.dispatcher

  IO(Tcp)(context.system) ! Tcp.Bind(self, endpoint)

  println(s"Now listening on port $port")

  def handleCommand: PartialFunction[(String, Option[Int], ActorRef), Unit]

  protected def reply(message: String, receiver: ActorRef) = {
    receiver ! Tcp.Write(ByteString(message))
    receiver ! Tcp.Close
  }

  protected def closeOnError(receiver: ActorRef): PartialFunction[Throwable, Unit] = {
    case err: Throwable =>
      receiver ! Tcp.Close
  }

  def receive: Receive = {
    case Tcp.Connected(remote, _) =>
      sender ! Tcp.Register(self)

    case Tcp.Received(bs) =>
      val content = bs.utf8String

      content match {
        case command(c, value) if handleCommand.isDefinedAt(c, Some(value.toInt), sender) =>
          handleCommand(c, Some(value.toInt), sender)
        case c if c.startsWith("quit") =>
          context.system.terminate()
        case c if handleCommand.isDefinedAt(c, None, sender) =>
          handleCommand(c, None, sender)
        case _ =>
          sender ! Tcp.Close
      }

    case Tcp.Closed =>
    case Tcp.PeerClosed =>
  }
}
