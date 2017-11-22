package com.github.bhop.sparxer.http

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.util.Timeout
import monix.eval.Task

import com.github.bhop.sparxer.protocol.engine.SparkEngine._
import com.github.bhop.sparxer.protocol.spark.Spark.JobConfig

trait SparkEngineProxy {
  import SparkEngineProxy._
  def submit(job: JobConfig): Task[Job]
}

object SparkEngineProxy {
  case class Job(id: String)
  case class SparkEngineError(message: String) extends RuntimeException
}

class ClusterAwareSparkEngineProxy(engine: ActorRef[SparkEngineCommand])
                                  (implicit scheduler: Scheduler, timeout: Timeout) extends SparkEngineProxy {

  import SparkEngineProxy._
  import akka.typed.scaladsl.AskPattern._

  override def submit(job: JobConfig): Task[Job] =
    Task.fromFuture[SparkEngineEvent](engine ? (SubmitJob(job, _))).flatMap {
      case JobSubmitted(id) => Task.now(Job(id))
      case _ => Task.raiseError(SparkEngineError("Could not get job details"))
    }
}

object ClusterAwareSparkEngineProxy {
  def apply(router: ActorRef[SparkEngineCommand])
           (implicit scheduler: Scheduler, timeout: Timeout): ClusterAwareSparkEngineProxy =
    new ClusterAwareSparkEngineProxy(router)
}
