package com.github.bhop.sparxer.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import com.github.bhop.sparxer.adapter.domain.JobConfig
import com.github.bhop.sparxer.http.adapters.SparkEngineAdapter
import com.typesafe.scalalogging.StrictLogging
import monix.execution.Scheduler

import scala.util.control.NonFatal
import scala.util.{Failure, Success}

trait Routing extends JsonSupport with StrictLogging {

  import Routing._

  def routes(engine: SparkEngineAdapter)(implicit S: Scheduler): Route =
    logRequestResult("sparxer-http-server") {
      path("api" / "jobs" / "submit") {
        post {
          entity(as[JobConfig]) { config =>
            onComplete(engine.submit(config).runAsync) {
              case Success(_) => complete("job submitted")
              case Failure(error) => throw error
            }
          }
        }
      }
    }

  implicit val exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case NonFatal(ex) =>
        extractUri { uri =>
          logger.error(s"Request to $uri could not be handled normally: ${ex.getMessage}")
          complete(StatusCodes.InternalServerError -> ErrorResponse(error = ex.getMessage))
        }
    }
}

object Routing {
  case class ErrorResponse(error: String)
}
