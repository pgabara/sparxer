package com.github.bhop.sparxer.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import com.github.bhop.sparxer.adapter.domain.{JobConfig, SparkApp}
import com.github.bhop.sparxer.http.Routing.ErrorResponse

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat1(ErrorResponse)
  implicit val sparkAppFormat: RootJsonFormat[SparkApp] = jsonFormat4(SparkApp)
  implicit val jobConfigFormat: RootJsonFormat[JobConfig] = jsonFormat2(JobConfig)
}