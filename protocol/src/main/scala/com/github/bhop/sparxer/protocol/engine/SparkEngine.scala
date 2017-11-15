package com.github.bhop.sparxer.protocol.engine

import akka.typed.ActorRef
import akka.typed.receptionist.Receptionist.ServiceKey
import com.github.bhop.sparxer.protocol.spark.Spark.JobConfig

object SparkEngine {

  val SparkEngineReceptionistKey: ServiceKey[SparkEngineCommand] = ServiceKey[SparkEngineCommand]("SparkEngineKey")

  sealed trait SparkEngineCommand
  case class SubmitJob(config: JobConfig, replyTo: ActorRef[SparkEngineEvent]) extends SparkEngineCommand

  sealed trait SparkEngineEvent
  case class JobSubmitted(id: String) extends SparkEngineEvent
}
