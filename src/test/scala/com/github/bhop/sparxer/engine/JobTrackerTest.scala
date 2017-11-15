package com.github.bhop.sparxer.engine

import akka.typed.testkit.{EffectfulActorContext, Inbox}
import com.github.bhop.sparxer.adapter.domain.{JobState, JobSubscription}
import com.github.bhop.sparxer.engine.JobTracker.{Command, Event, JobStateUpdated}
import monix.reactive.Observable
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import monix.execution.Scheduler.Implicits.global

class JobTrackerTest extends WordSpec with Matchers with ScalaFutures {

  "A Job Tracker" should {

    "notify one recipient once job state is changed" in {
      val inbox = Inbox[Event]("job-multi-updates")
      val tracker = JobTracker(subscription("1", List("RUNNING", "FINISHED")), Set(inbox.ref))
      val context = new EffectfulActorContext[Command]("job-multi-updates", tracker, 100, null)

      val messages = context.selfInbox.receiveAll()
      messages.size should be(3)

      messages.foreach(context.run)
      inbox.receiveAll() should equal (List(
        JobStateUpdated("1", "RUNNING"), JobStateUpdated("1", "FINISHED")
      ))
    }

    "notify many recipients once job state is changed" in {
      val inbox1 = Inbox[Event]("job-updates-1")
      val inbox2 = Inbox[Event]("job-updates-2")
      val tracker = JobTracker(subscription("1", List("FINISHED")), Set(inbox1.ref, inbox2.ref))
      val context = new EffectfulActorContext[Command]("job-updates", tracker, 100, null)

      val messages = context.selfInbox.receiveAll()
      messages.size should be(2)

      messages.foreach(context.run)
      inbox1.receiveMsg() should be(JobStateUpdated("1", "FINISHED"))
      inbox2.receiveMsg() should be(JobStateUpdated("1", "FINISHED"))
    }

    "stop once final state received" in {
      val inbox = Inbox[Event]("job-terminates")
      val tracker = JobTracker(subscription("1", List("FINISHED")), Set(inbox.ref))
      val context = new EffectfulActorContext[Command]("job-terminates", tracker, 100, null)
      val messages = context.selfInbox.receiveAll()

      messages.foreach(context.run)
      context.isAlive should be(false)
    }
  }

  def subscription(id: String, states: Seq[String]): JobSubscription =
    JobSubscription(id, Observable.fromIterable(states.map(JobState(None, _))))
}
