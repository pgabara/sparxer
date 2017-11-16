package com.github.bhop.sparxer.engine

import akka.typed.receptionist.Receptionist.{Listing, Subscribe}
import akka.typed.testkit.{EffectfulActorContext, Inbox}
import com.github.bhop.sparxer.AkkaBehaviourTest
import monix.eval.Task
import monix.reactive.Observable
import org.scalatest.{Matchers, WordSpec}
import com.github.bhop.sparxer.engine.SparkEngine.{Command, Submit}
import com.github.bhop.sparxer.adapter.domain.{JobConfig, JobState, JobSubscription, SparkApp}
import monix.execution.Scheduler.Implicits.global

class SparkEngineTest extends WordSpec with Matchers with AkkaBehaviourTest {

  "A Spark Engine" should {

    "spawn child to track job progress" in withActorSystem { system =>
      val context = new EffectfulActorContext[Command]("spawn-tracker", SparkEngine(stubSubmit), 100, system)
      context.run(Submit(stubJobConfig))
      context.child("JobTracker-1").isDefined should be(true)
    }

    "register itself in the receptionist service" in withActorSystem { system =>
      val inbox = Inbox[Listing[Command]]("receptionist-listing")
      system.receptionist ! Subscribe[Command](SparkEngine.ReceptionistKey, inbox.ref)
      val context = new EffectfulActorContext[Command]("receptionist-listing", SparkEngine(stubSubmit), 100, system)
      eventually {
        inbox.receiveAll() should contain(Listing[Command](SparkEngine.ReceptionistKey, Set(context.self)))
      }
    }
  }

  def stubJobConfig: JobConfig =
    JobConfig(
      app = SparkApp("test", "test.jar", "Test"),
      verbose = false
    )

  def stubSubmit(config: JobConfig): Task[JobSubscription] =
    Task.now(JobSubscription(
      subscriptionId = "1",
      states = Observable.fromIterable(List(JobState(None, "RUNNING"), JobState(None, "FINISHED")))
    ))
}
