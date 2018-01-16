package com.github.bhop.sparxer.http.proxy

import akka.actor.Scheduler
import akka.typed.ActorRef
import akka.util.Timeout
import com.github.bhop.sparxer.protocol.engine.SparkEngine._
import com.github.bhop.sparxer.protocol.spark.Spark.JobConfig
import monix.eval.Task

class ClusterAwareSparkEngineProxy(engine: ActorRef[SparkEngineCommand])
                                  (implicit scheduler: Scheduler, timeout: Timeout) extends SparkEngineProxy {

  import SparkEngineProxy._
  import akka.typed.scaladsl.AskPattern._

  override def submit(job: JobConfig): Task[Job] =
    Task.fromFuture[SparkEngineEvent](engine ? (SubmitJob(job, _))).flatMap {
      case JobSubmitted(id) => Task.now(Job(id))
    }
}

object ClusterAwareSparkEngineProxy {
  def apply(router: ActorRef[SparkEngineCommand])
           (implicit scheduler: Scheduler, timeout: Timeout): ClusterAwareSparkEngineProxy =
    new ClusterAwareSparkEngineProxy(router)
}

