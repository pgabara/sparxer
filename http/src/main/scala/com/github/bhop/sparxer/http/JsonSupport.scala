package com.github.bhop.sparxer.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import com.github.bhop.sparxer.http.JsonSupport.ErrorResponse
import com.github.bhop.sparxer.http.SparkEngineProxy.Job
import com.github.bhop.sparxer.protocol.spark.Spark.{JobConfig, SparkApp}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat1(ErrorResponse)
  implicit val sparkAppFormat: RootJsonFormat[SparkApp]           = jsonFormat4(SparkApp)
  implicit val jobConfigFormat: RootJsonFormat[JobConfig]         = jsonFormat2(JobConfig)
  implicit val jobFormat: RootJsonFormat[Job]                     = jsonFormat1(Job)
}

object JsonSupport {
  case class ErrorResponse(message: String)
}
