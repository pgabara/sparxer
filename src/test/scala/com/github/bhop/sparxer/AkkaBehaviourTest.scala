package com.github.bhop.sparxer

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import org.scalatest.concurrent.{Eventually, ScalaFutures}

import scala.concurrent.duration._

trait AkkaBehaviourTest extends ScalaFutures with Eventually {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 1.minute)

  def withActorSystem(f: ActorSystem[Nothing] => Unit): Unit = {
    val system: ActorSystem[Nothing] = ActorSystem(Actor.ignore, "test")
    try {
      f(system)
    } finally system.terminate().futureValue
  }
}
