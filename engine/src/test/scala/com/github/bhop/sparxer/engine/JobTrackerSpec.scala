package com.github.bhop.sparxer.engine

import akka.typed.testkit.{EffectfulActorContext, Inbox}
import monix.reactive.Observable
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import com.github.bhop.sparxer.protocol.engine.JobTracker.{JobStateUpdated, JobTrackerCommand, JobTrackerEvent}
import com.github.bhop.sparxer.protocol.spark.Spark.{JobState, JobSubscription}
import monix.execution.Scheduler

class JobTrackerSpec extends WordSpec with Matchers with ScalaFutures {

  implicit val io: Scheduler = Scheduler.io()

  "A Job Tracker" should {

    "notify one recipient once job state is updated" in {
      val inbox = Inbox[JobTrackerEvent]("job-multi-updates")
      val tracker = JobTracker(subscription("1", List("RUNNING", "FINISHED")), Set(inbox.ref))
      val context = new EffectfulActorContext[JobTrackerCommand]("job-multi-updates", tracker, 100, null)

      val messages = context.selfInbox.receiveAll()
      messages.size should be(3)

      messages.foreach(context.run)
      inbox.receiveAll() should equal (List(
        JobStateUpdated(id = "1", JobState(None, "RUNNING")), JobStateUpdated(id = "1", JobState(None, "FINISHED"))
      ))
    }

    "stop once final state received" in {
      val inbox = Inbox[JobTrackerEvent]("job-terminates")
      val tracker = JobTracker(subscription("1", List("FINISHED")), Set(inbox.ref))
      val context = new EffectfulActorContext[JobTrackerCommand]("job-terminates", tracker, 100, null)
      val messages = context.selfInbox.receiveAll()

      messages.foreach(context.run)
      context.isAlive should be(false)
    }
  }

  private def subscription(id: String, states: Seq[String]): JobSubscription =
    JobSubscription(id, Observable.fromIterable(states.map(JobState(None, _))))
}
