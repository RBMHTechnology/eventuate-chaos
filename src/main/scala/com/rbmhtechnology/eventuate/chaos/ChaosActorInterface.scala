package com.rbmhtechnology.eventuate.chaos

import akka.actor.ActorRef
import akka.io.Tcp
import akka.util.ByteString
import akka.pattern.ask
import akka.util.Timeout
import com.rbmhtechnology.eventuate.chaos.ChaosActorInterface.HealthCheckResult
import com.rbmhtechnology.eventuate.chaos.ChaosActorInterface.HealthCheck

import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success


object ChaosActorInterface {
  case class HealthCheck(requester: ActorRef)
  case class HealthCheckResult(state: Int, requester: ActorRef)
}

class ChaosActorInterface(chaosActor: ActorRef) extends ChaosInterface {
  implicit val timeout = Timeout(1.seconds)

  def handleCommand = {
    case ("persist", None, recv) =>
      val check = HealthCheck(recv)

      (chaosActor ? check).mapTo[HealthCheckResult] onComplete {
        case Success(result) =>
          result.requester ! Tcp.Write(ByteString(result.state.toString))
          result.requester ! Tcp.Close
        case Failure(e) =>
          recv ! Tcp.Close
      }
  }
}
