package com.github.bhop.sparxer.http.adapters

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.testkit.{EffectfulActorContext, Inbox}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import com.github.bhop.sparxer.adapter.domain.{JobConfig, SparkApp}
import com.github.bhop.sparxer.engine.SparkEngine
import com.github.bhop.sparxer.http.adapters.ClusterSparkEngine._

class ClusterSparkEngineTest extends WordSpec with Matchers with ScalaFutures {

  "A Cluster Spark Engine" should {

    "update spark engine instances once notification from receptionist received" ignore {
      // todo: find a way to control receptionist Listing messages in tests
    }

    "return an error if no active spark engine instances registered" in withActorSystem { system =>
      val inbox = Inbox[Event]("no-active-engines")
      val context = new EffectfulActorContext[Command]("no-active-engines", ClusterSparkEngine.behavior, 100, system)
      context.run(Submit(JobConfig(SparkApp("test", "test.jar", "Test")), inbox.ref))
      inbox.receiveMsg() should be(Error("There are no registered spark engines in the cluster"))
    }

    "return an acknowledge once request to spark engine is sent" in withActorSystem { system =>
      val inbox = Inbox[Event]("ack")
      val behavior = ClusterSparkEngine.active(Vector(Inbox[SparkEngine.Command]("engine").ref))
      val context = new EffectfulActorContext[Command]("ack", behavior, 100, system)
      context.run(Submit(JobConfig(SparkApp("test", "test.jar", "Test")), inbox.ref))
      inbox.receiveMsg() should be(Submitted)
    }

    "send request to only one of spark engine instance" in withActorSystem { system =>
      val engine1 = Inbox[SparkEngine.Command]("engine-1")
      val engine2 = Inbox[SparkEngine.Command]("engine-2")
      val behavior = ClusterSparkEngine.active(Vector(engine1.ref, engine2.ref))
      val context = new EffectfulActorContext[Command]("routing", behavior, 100, system)
      context.run(Submit(JobConfig(SparkApp("test", "test.jar", "Test")), Inbox[Event]("routing").ref))
      val messages = engine1.receiveAll() ++ engine2.receiveAll()
      messages.size should be(1)
      messages.head should be(SparkEngine.Submit(JobConfig(SparkApp("test", "test.jar", "Test"))))
    }
  }

  def withActorSystem(f: ActorSystem[Nothing] => Unit): Unit = {
    val system: ActorSystem[Nothing] = ActorSystem(Actor.ignore, "test")
    try {
      f(system)
    } finally system.terminate().futureValue
  }
}
