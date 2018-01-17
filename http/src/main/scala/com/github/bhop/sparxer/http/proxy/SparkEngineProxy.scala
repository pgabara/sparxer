package com.github.bhop.sparxer.http.proxy

import monix.eval.Task
import com.github.bhop.sparxer.protocol.spark.Spark.JobConfig

trait SparkEngineProxy {
  import SparkEngineProxy._
  def submit(job: JobConfig): Task[Job]
}

object SparkEngineProxy {
  case class Job(id: String)
  case class SparkEngineError(message: String) extends RuntimeException
}