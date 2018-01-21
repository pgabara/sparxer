package com.github.bhop.sparxer.engine.spark

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}

class SparkSubmitSpec extends WordSpec with Matchers with ScalaFutures with SparkOps {

  import monix.execution.Scheduler.Implicits.global

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(20, Millis))

  "A Spark Submit" should {

    "submit a job and track states" in {
      val states = SparkSubmit(testSparkInstance).submit(testJobConfig).map(_.states)
      val isNonEmpty = states.flatMap(_.nonEmptyL)
      whenReady(isNonEmpty.runAsync) { _ should be { true } }
    }

    "return FINISHED state once the job is done" in {
      val states = SparkSubmit(testSparkInstance).submit(testJobConfig).map(_.states)
      val lastState = states.flatMap(_.lastL)
      whenReady(lastState.runAsync) { _.state should be { "FINISHED" } }
    }
  }
}
