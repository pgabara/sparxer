package com.github.bhop.sparxer.http.adapters

import akka.{Done, actor}
import akka.typed.Behavior
import akka.typed.receptionist.Receptionist.Listing
import akka.typed.scaladsl.Actor
import akka.typed.testkit.{EffectfulActorContext, Inbox}
import akka.util.Timeout
import com.github.bhop.sparxer.AkkaBehaviourTest
import org.scalatest.{Matchers, WordSpec}
import com.github.bhop.sparxer.adapter.domain.{JobConfig, SparkApp}
import com.github.bhop.sparxer.engine.SparkEngine
import com.github.bhop.sparxer.http.adapters.ClusterSparkEngine._
import monix.execution.Scheduler

import scala.concurrent.duration._

class ClusterSparkEngineTest extends WordSpec with Matchers with AkkaBehaviourTest {

  "A Cluster Spark Engine" should {

    "update spark engine instances once notification from receptionist received" in withActorSystem { system =>
      val inbox = Inbox[Event]("update-engines")
      val context = new EffectfulActorContext[Command]("update-engines", ClusterSparkEngine.behavior, 100, system)

      context.run(Submit(JobConfig(SparkApp("test", "test.jar", "Test")), inbox.ref))
      inbox.receiveMsg() should be(Error("There are no registered spark engines in the cluster"))

      val listing = Listing[SparkEngine.Command](SparkEngine.ReceptionistKey, Set(Inbox[SparkEngine.Command]("engine").ref))
      context.run(UpdateInstances(listing))

      context.run(Submit(JobConfig(SparkApp("test", "test.jar", "Test")), inbox.ref))
      inbox.receiveMsg() should be(Submitted)
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

  "A Cluster Spark Engine adapter" should {

    implicit val io: Scheduler = Scheduler.io()
    implicit val timeout: Timeout = Timeout(1.minute)

    "return acknowledge if given job registered in spark engine successfully" in withActorSystem { system =>
      implicit val scheduler: actor.Scheduler = system.scheduler
      val engineRef = system.systemActorOf(successEngine, "engine").futureValue
      val submission = new ClusterSparkEngine(engineRef).submit(JobConfig(SparkApp("test", "test.jar", "Test")))
      submission.runAsync.futureValue should be(Done)
    }

    "provide exception if spark engine rises an error" in withActorSystem { system =>
      implicit val scheduler: actor.Scheduler = system.scheduler
      val engineRef = system.systemActorOf(failureEngine, "engine").futureValue
      val submission = new ClusterSparkEngine(engineRef).submit(JobConfig(SparkApp("test", "test.jar", "Test")))
      intercept[RuntimeException] {
        submission.runAsync.futureValue
      }
    }
  }

  private def successEngine: Behavior[Command] =
    Actor.immutable[Command] { (_, message) =>
      message match {
        case Submit(_, replayTo) =>
          replayTo ! Submitted
          Actor.same
        case _ => Actor.same
      }
    }

  private def failureEngine: Behavior[Command] =
    Actor.immutable[Command] { (_, message) =>
      message match {
        case Submit(_, replayTo) =>
          replayTo ! Error("Error message")
          Actor.same
        case _ => Actor.same
      }
    }
}
