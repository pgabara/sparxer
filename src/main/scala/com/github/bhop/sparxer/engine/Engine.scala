package com.github.bhop.sparxer.engine

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.typed.Behavior
import akka.typed.scaladsl.adapter._
import akka.typed.scaladsl.Actor
import monix.execution.Scheduler

import com.github.bhop.sparxer.adapter.{SparkSubmit, domain}
import com.github.bhop.sparxer.core.Node

object Engine extends App with Node {

  val config = nodeConfig(args, resourceBasename = "engine")

  implicit val io: Scheduler = Scheduler.io()
  val system = ActorSystem("Sparxer", config)

  system.spawn[Nothing](guardian, "Guardian")

  logger.info("Engine is up and running. Press [Return] to stop...")
  scala.io.StdIn.readLine()
  system.terminate().onComplete(_ => logger.info("Engine terminated"))

  private def guardian: Behavior[Nothing] =
    Actor.deferred[Nothing] { context =>
      val spark = domain.Spark(
        home = Paths.get(config.getString("engine.spark.home")),
        mode = config.getString("engine.spark.mode"),
        master = config.getString("engine.spark.master"))
      val sparkSubmit = new SparkSubmit(spark)
      context.spawn(SparkEngine(sparkSubmit.submit), "JobSubmitter")
      Actor.ignore
    }
}
