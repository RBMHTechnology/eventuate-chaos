package com.rbmhtechnology.eventuate.chaos

import java.net.InetSocketAddress

import akka.actor.Actor
import akka.actor.ActorRef
import akka.io.IO
import akka.io.Tcp
import akka.util.ByteString
import akka.pattern.ask
import akka.util.Timeout
import com.rbmhtechnology.eventuate.chaos.ChaosActorInterface.HealthCheckResult
import scala.concurrent.duration._
import com.rbmhtechnology.eventuate.chaos.ChaosActorInterface.HealthCheck

import scala.util.Failure
import scala.util.Success


object ChaosActorInterface {
  case class HealthCheck(requester: ActorRef)
  case class HealthCheckResult(state: Int, requester: ActorRef)
}

class ChaosActorInterface(chaosActor: ActorRef) extends Actor {
  val port = 8080
  val endpoint = new InetSocketAddress(port)

  implicit val ec = context.dispatcher
  implicit val timeout = Timeout(1.seconds)

  IO(Tcp)(context.system) ! Tcp.Bind(self, endpoint)

  println(s"Now listening on port $port")

  def receive: Receive = {
    case Tcp.Connected(remote, _) =>
      sender ! Tcp.Register(self)

    case Tcp.Received(bs) =>
      val content = bs.utf8String

      if (content.startsWith("persist")) {
        val requester = sender
        val check = HealthCheck(requester)

        (chaosActor ? check).mapTo[HealthCheckResult] onComplete {
          case Success(result) =>
            result.requester ! Tcp.Write(ByteString(result.state.toString))
            result.requester ! Tcp.Close
          case Failure(e) =>
            requester ! Tcp.Close
        }
      } else if (content.startsWith("quit")) {
        context.system.terminate()
      } else {
        sender ! Tcp.Close
      }

    case Tcp.Closed =>
    case Tcp.PeerClosed =>
  }
}
