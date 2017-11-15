package com.github.bhop.sparxer.http

import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.http.scaladsl.server.Directives._
import monix.execution.Scheduler
import com.github.bhop.sparxer.http.JsonSupport.ErrorResponse
import com.github.bhop.sparxer.protocol.spark.Spark.JobConfig

import scala.util.{Failure, Success}
import scala.util.control.NonFatal

trait Routing extends JsonSupport {

  def routes(engine: SparkEngineProxy)(implicit io: Scheduler): Route =
    pathPrefix("api" / "jobs") {
      path("submit") {
        post {
          entity(as[JobConfig]) { config =>
            onComplete(engine.submit(config).runAsync) {
              case Success(job) => complete(job)
              case Failure(ex)  => throw ex
            }
          }
        }
      }
    }

  implicit def exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case NonFatal(error) =>
        complete(ErrorResponse(message = error.getMessage))
    }
}