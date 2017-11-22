package com.github.bhop.sparxer.engine

import akka.typed.ActorSystem
import akka.typed.receptionist.Receptionist.{Listing, Subscribe}
import akka.typed.scaladsl.Actor
import akka.typed.testkit.{EffectfulActorContext, Inbox}
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{Matchers, WordSpec}

import com.github.bhop.sparxer.protocol.engine.SparkEngine._
import com.github.bhop.sparxer.protocol.spark.Spark._

import scala.concurrent.duration._

class SparkEngineSpec extends WordSpec with Matchers with ScalaFutures with Eventually {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 10.seconds)
  implicit val io: Scheduler = Scheduler.io()

  "A Spark Engine" should {

    "spawn child to track job progress" in withActorSystem { system =>
      val context = new EffectfulActorContext[SparkEngineCommand]("spawn-tracker", SparkEngine(stubSubmit), 100, system)
      val jobConfig = JobConfig(SparkApp(name = "test", jar = "test.jar", main = "Test"))
      context.run(SubmitJob(jobConfig, Inbox[SparkEngineEvent]("child").ref))
      context.child("JobTracker-1").isDefined should be(true)
    }

    "return an acknowledge with a job subscription id" in withActorSystem { system =>
      val inbox = Inbox[SparkEngineEvent]("ack")
      val context = new EffectfulActorContext[SparkEngineCommand]("ack", SparkEngine(stubSubmit), 100, system)
      val jobConfig = JobConfig(SparkApp(name = "test", jar = "test.jar", main = "Test"))
      context.run(SubmitJob(jobConfig, inbox.ref))
      inbox.receiveMsg() should be(JobSubmitted(id = "1"))
    }

    "register itself in the receptionist service" in withActorSystem { system =>
      val inbox = Inbox[Listing[SparkEngineCommand]]("listing")
      system.receptionist ! Subscribe[SparkEngineCommand](SparkEngineReceptionistKey, inbox.ref)
      val context = new EffectfulActorContext[SparkEngineCommand]("listing", SparkEngine(stubSubmit), 100, system)
      eventually {
        inbox.receiveAll() should contain(Listing[SparkEngineCommand](SparkEngineReceptionistKey, Set(context.self)))
      }
    }
  }

  private def stubSubmit(config: JobConfig): Task[JobSubscription] =
    Task.now(JobSubscription(
      id = "1",
      states = Observable.fromIterable(List(JobState(None, "RUNNING"), JobState(None, "FINISHED")))
    ))

  def withActorSystem(f: ActorSystem[Nothing] => Unit): Unit = {
    val system: ActorSystem[Nothing] = ActorSystem(Actor.ignore, "test")
    try {
      f(system)
    } finally system.terminate().futureValue
  }
}
