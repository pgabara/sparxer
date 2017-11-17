package com.github.bhop.sparxer.http

import akka.Done
import akka.http.scaladsl.testkit.ScalatestRouteTest
import monix.eval.Task
import org.scalatest.{Matchers, WordSpec}
import monix.execution.Scheduler

import com.github.bhop.sparxer.adapter.domain
import com.github.bhop.sparxer.adapter.domain.{JobConfig, SparkApp}
import com.github.bhop.sparxer.http.adapters.SparkEngineAdapter

class RoutingTest extends WordSpec with Matchers with ScalatestRouteTest with Routing {

  val io: Scheduler = Scheduler.io()

  "A Routing" when {

    "submitting new job" should {

      "return notification that job has been submitted successfully" in {
        val config = JobConfig(app = SparkApp("test", "test.jar", "Test"), verbose = false)
        Post("/api/jobs/submit", config) ~> routes(successEngine)(io) ~> check {
          responseAs[String] should be("job submitted")
        }
      }

      "return an error when something went wrong" in {
        val config = JobConfig(app = SparkApp("test", "test.jar", "Test"), verbose = false)
        val error = new IllegalArgumentException("Internal state error")
        Post("/api/jobs/submit", config) ~> routes(failingEngine(error))(io) ~> check {
          responseAs[Routing.ErrorResponse] should be(Routing.ErrorResponse(error = "Internal state error"))
        }
      }
    }
  }

  private def successEngine = new SparkEngineAdapter {
    override def submit(config: domain.JobConfig): Task[Done] = Task.now(Done)
  }

  private def failingEngine(error: Throwable) = new SparkEngineAdapter {
    override def submit(config: domain.JobConfig): Task[Done] = Task.raiseError(error)
  }
}
