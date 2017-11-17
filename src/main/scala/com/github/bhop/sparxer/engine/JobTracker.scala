package com.github.bhop.sparxer.engine

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.Actor
import com.typesafe.scalalogging.StrictLogging
import monix.execution.Scheduler

import com.github.bhop.sparxer.adapter.domain

object JobTracker extends StrictLogging {

  sealed trait Command
  private case class NotifyUpdateState(state: domain.JobState) extends Command
  private case object Terminate extends Command

  sealed trait Event
  case class JobStateUpdated(subscriptionId: String, state: String) extends Event

  def apply(subscription: domain.JobSubscription, replyTo: Set[ActorRef[Event]])
                        (implicit io: Scheduler): Behavior[Command] =
    Actor.deferred { context =>

      subscription.states
        .map { context.self ! NotifyUpdateState(_) }
        .doOnComplete { () => context.self ! Terminate }
        .runAsyncGetLast

      Actor.immutable { (_, message) =>
        message match {
          case NotifyUpdateState(update) =>
            logger.info(s"Spark job state updated: ${update.state}. Subscription id: ${subscription.subscriptionId}")
            replyTo.foreach(_ ! JobStateUpdated(subscription.subscriptionId, update.state))
            Actor.same

          case Terminate =>
            logger.info(s"Spark job completed. Subscription id: ${subscription.subscriptionId}")
            Actor.stopped
        }
      }
    }
}
