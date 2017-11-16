package com.github.bhop.sparxer

import akka.typed.ActorSystem
import akka.typed.scaladsl.Actor
import org.scalatest.concurrent.{Eventually, ScalaFutures}

trait AkkaBehaviourTest extends ScalaFutures with Eventually {

  def withActorSystem(f: ActorSystem[Nothing] => Unit): Unit = {
    val system: ActorSystem[Nothing] = ActorSystem(Actor.ignore, "test")
    try {
      f(system)
    } finally system.terminate().futureValue
  }
}
