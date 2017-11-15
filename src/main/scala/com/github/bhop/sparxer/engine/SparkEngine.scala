package com.github.bhop.sparxer.engine

import akka.typed.receptionist.Receptionist.{Register, ServiceKey}
import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.Actor
import monix.execution.Scheduler
import com.github.bhop.sparxer.adapter.domain
import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task

object SparkEngine extends StrictLogging {

  sealed trait Command
  case class Submit(config: domain.JobConfig, replyTo: Set[ActorRef[JobTracker.Event]] = Set.empty) extends Command

  val ReceptionistKey: ServiceKey[Command] = ServiceKey[Command]("SparkEngineKey")

  def apply(submit: domain.JobConfig => Task[domain.JobSubscription])
                       (implicit io: Scheduler): Behavior[Command] =
    Actor.deferred { context =>
      context.system.receptionist ! Register(ReceptionistKey, context.self, context.system.deadLetters)
      Actor.immutable { (context, message) =>
        message match {
          case Submit(config, replyTo) =>
            submit(config).foreach { subscription =>
              logger.info(s"Submitted new spark job. App name: ${config.app.name}. Subscription id: ${subscription.subscriptionId}")
              context.spawn(JobTracker(subscription, replyTo), s"JobTracker-${subscription.subscriptionId}")
            }
            Actor.same
        }
      }
    }
}
