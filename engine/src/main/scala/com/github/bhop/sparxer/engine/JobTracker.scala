package com.github.bhop.sparxer.engine

import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior}
import com.github.bhop.sparxer.protocol.engine.JobTracker._
import com.github.bhop.sparxer.protocol.spark.Spark.{JobState, JobSubscription}
import monix.execution.Scheduler

object JobTracker {

  def apply(subscription: JobSubscription, replyTo: Set[ActorRef[JobTrackerEvent]] = Set.empty)
           (implicit io: Scheduler): Behavior[JobTrackerCommand] =
    Actor.deferred { context =>
      subscription.states
        .doOnComplete { () => context.self ! TerminateJobTracker }
        .foreach { state => context.self ! UpdateJobState(state) }

      active(subscription.id, undefinedState, replyTo)
    }

  private def active(id: String, state: JobState, replyTo: Set[ActorRef[JobTrackerEvent]]): Behavior[JobTrackerCommand] =
    Actor.immutable { (_, message) =>
      message match {
        case UpdateJobState(newState) =>
          replyTo.foreach(_ ! JobStateUpdated(id, newState))
          active(id, newState, replyTo)

        case TerminateJobTracker =>
          Actor.stopped
      }
    }

  private def undefinedState: JobState = JobState(jobId = None, state = "UNDEFINED")
}
