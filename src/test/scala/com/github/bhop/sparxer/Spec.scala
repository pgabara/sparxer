package com.github.bhop.sparxer

import akka.actor._
import akka.testkit.TestKit
import akka.util.Timeout
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

trait Spec extends WordSpecLike with Matchers with BeforeAndAfterAll

trait AkkaSpec extends Spec {
  this: TestKit =>

  def askUntil[R: ClassTag](ref: ActorRef, msg: Any)(p: R => Boolean)(implicit timeout: Timeout): Unit = {
    import akka.pattern.ask
    awaitCond({
      val response = Await.result((ref ? msg).mapTo[R], Duration.Inf)
      p(response)
    })
  }
}
