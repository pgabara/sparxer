package com.github.bhop.sparxer.http

import akka.typed.{ActorRef, ActorSystem}
import akka.typed.receptionist.Receptionist.{Listing, ServiceKey}
import akka.typed.scaladsl.Actor
import akka.typed.testkit.{EffectfulActorContext, Inbox}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

class ClusterRouterSpec extends WordSpec with Matchers with ScalaFutures {

  import ClusterRouterSpec._

  "A Cluster aware router" should {

    "return nothing if no routees" in withActorSystem { system =>
      val inbox = Inbox[Event]("inbox-1")
      val context = new EffectfulActorContext[Command]("context-1", ClusterRouter(Key), 100, system)
      context.run(Request(inbox.ref))
      inbox.hasMessages should be(false)
    }

    "pass message to a routee" in withActorSystem { system =>
      val context = new EffectfulActorContext[Command]("context-2", ClusterRouter(Key), 100, system)

      val routee1 = Inbox[Command]("routee-1")
      registerRoutee(routee1, Key, context)

      val routee2 = Inbox[Command]("routee-2")
      registerRoutee(routee2, Key, context)

      val routee3 = Inbox[Command]("routee-3")
      registerRoutee(routee3, Key, context)


      val inbox = Inbox[Event]("inbox-2")
      context.run(Request(inbox.ref))

      val messages = routee1.receiveAll() ++ routee2.receiveAll() ++ routee3.receiveAll()
      messages.size should be(1)
      messages.head should be(Request(inbox.ref))
    }
  }

  def registerRoutee[T](routee: Inbox[T], key: ServiceKey[T], context: EffectfulActorContext[T]): Unit = {
    context.self.upcast ! Listing[T](key, Set(routee.ref))
    context.run(context.selfInbox.receiveMsg())
  }

  def withActorSystem(f: ActorSystem[Nothing] => Unit): Unit = {
    val system: ActorSystem[Nothing] = ActorSystem(Actor.ignore, "test")
    try {
      f(system)
    } finally system.terminate().futureValue
  }
}

object ClusterRouterSpec {

  val Key: ServiceKey[Command] = ServiceKey[Command]("Test")

  sealed trait Command
  case class Request(replyTo: ActorRef[Event]) extends Command

  sealed trait Event
  case object Response extends Event
}
