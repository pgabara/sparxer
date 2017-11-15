package com.github.bhop.sparxer.http

import akka.http.scaladsl.testkit.ScalatestRouteTest
import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest.{Matchers, WordSpec}
import com.github.bhop.sparxer.http.JsonSupport.ErrorResponse
import com.github.bhop.sparxer.http.SparkEngineProxy.Job
import com.github.bhop.sparxer.protocol.spark.Spark.{JobConfig, SparkApp}

class RoutingSpec extends WordSpec with Matchers with ScalatestRouteTest with Routing {

  implicit val io: Scheduler = Scheduler.io()

  "A Routing" when {

    "POST: /api/jobs/submit (submit new spark job)" should {
      "return notification that job has been submitted successfully" in {
        val config = JobConfig(app = SparkApp("test", "test.jar", "Test"), verbose = false)
        Post("/api/jobs/submit", config) ~> routes(successEngine)(io) ~> check {
          responseAs[Job] should be(Job(id = "1"))
        }
      }

      "return an error when something went wrong" in {
        val config = JobConfig(app = SparkApp("test", "test.jar", "Test"), verbose = false)
        val error = new IllegalArgumentException("Internal state error")
        Post("/api/jobs/submit", config) ~> routes(failingEngine(error))(io) ~> check {
          responseAs[ErrorResponse] should be(ErrorResponse(message = "Internal state error"))
        }
      }
    }
  }

  private def successEngine = new SparkEngineProxy {
    override def submit(job: JobConfig): Task[Job] = Task.now(Job(id = "1"))
  }

  private def failingEngine(error: Throwable) = new SparkEngineProxy {
    override def submit(job: JobConfig): Task[Job] = Task.raiseError(error)
  }
}
