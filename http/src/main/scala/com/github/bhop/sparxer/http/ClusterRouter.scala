package com.github.bhop.sparxer.http

import java.util.concurrent.ThreadLocalRandom

import akka.typed.receptionist.Receptionist
import akka.typed.{ActorRef, Behavior}
import akka.typed.receptionist.Receptionist.{Listing, ServiceKey}
import akka.typed.scaladsl.Actor

object ClusterRouter {

  def apply[T](key: ServiceKey[T]): Behavior[T] =
    Actor.deferred[Any] { context ⇒
      context.system.receptionist ! Receptionist.Subscribe(key, context.self)

      def routingBehavior(routees: Vector[ActorRef[T]]): Behavior[Any] =
        Actor.immutable { (_, message) =>
          message match {
            case Listing(_, services: Set[ActorRef[T]] @unchecked) =>
              routingBehavior(services.toVector)
            case msg: T @unchecked =>
              if (routees.isEmpty)
                Actor.unhandled
              else {
                val i = ThreadLocalRandom.current.nextInt(routees.size)
                routees(i) ! msg
                Actor.same
              }
          }
        }

      routingBehavior(Vector.empty)
    }.narrow[T]
}