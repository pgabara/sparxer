package com.github.bhop.sparxer.engine

import akka.typed.Behavior
import akka.typed.receptionist.Receptionist.Register
import akka.typed.scaladsl.Actor
import monix.execution.Scheduler
import monix.eval.Task

import com.github.bhop.sparxer.protocol.engine.SparkEngine._
import com.github.bhop.sparxer.protocol.spark.Spark.{JobConfig, JobSubscription}

object SparkEngine {

  type SubmitFunction = JobConfig => Task[JobSubscription]

  def apply(submit: SubmitFunction)(implicit io: Scheduler): Behavior[SparkEngineCommand] =
    Actor.deferred { context =>
      context.system.receptionist ! Register(SparkEngineReceptionistKey, context.self, context.system.deadLetters)
      active(submit)
    }

  private def active(submit: SubmitFunction)(implicit io: Scheduler): Behavior[SparkEngineCommand] =
    Actor.immutable { (context, message) =>
      message match {
        case SubmitJob(job, replyTo) =>
          submit(job).foreach { subscription =>
            context.spawn(JobTracker(subscription), s"JobTracker-${subscription.id}")
            replyTo ! JobSubmitted(subscription.id)
          }
          Actor.same
      }
    }
}
