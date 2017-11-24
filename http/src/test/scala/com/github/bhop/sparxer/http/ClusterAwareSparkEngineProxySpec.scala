package com.github.bhop.sparxer.http

import akka.actor
import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.testkit.Inbox
import akka.util.Timeout
import monix.execution.Scheduler
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import com.github.bhop.sparxer.http.SparkEngineProxy.Job
import com.github.bhop.sparxer.protocol.engine.SparkEngine.{JobSubmitted, SparkEngineCommand, SubmitJob}
import com.github.bhop.sparxer.protocol.spark.Spark.{JobConfig, SparkApp}

import scala.concurrent.duration._

class ClusterAwareSparkEngineProxySpec extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  val system: ActorSystem[Nothing] = ActorSystem[Nothing](Actor.ignore, "test")
  implicit val scheduler: actor.Scheduler = system.scheduler
  implicit val timeout: Timeout = Timeout(1.second)
  implicit val io: Scheduler = Scheduler.io()

  "A Cluster aware Spark Engine Proxy" should {

    "return job details of submitted job" in {
      val router = Inbox[SparkEngineCommand]("router-1")
      val proxy = ClusterAwareSparkEngineProxy(router.ref)

      val response = proxy.submit(stubJob).runAsync

      val message = router.receiveMsg().asInstanceOf[SubmitJob]
      message.replyTo ! JobSubmitted("1")

      response.futureValue should be(Job("1"))
    }

    "return timeout error if no active spark engines" in {
      val router = Inbox[SparkEngineCommand]("router-3")
      val proxy = ClusterAwareSparkEngineProxy(router.ref)
      proxy.submit(stubJob).runSyncMaybe.isLeft should be(true)
    }
  }

  private def stubJob = JobConfig(SparkApp(name = "test", jar = "test.jar", main = "Test"))

  override def afterAll(): Unit = system.terminate()
}
