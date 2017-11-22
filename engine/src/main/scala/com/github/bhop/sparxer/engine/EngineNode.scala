package com.github.bhop.sparxer.engine

import akka.actor.ActorSystem
import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import akka.typed.scaladsl.adapter._
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import monix.execution.Scheduler
import com.github.bhop.sparxer.protocol.spark.Spark.SparkInstance
import com.github.bhop.sparxer.engine.spark.SparkSubmit

object EngineNode extends App with StrictLogging {

  implicit val io: Scheduler = Scheduler.io()

  val config = nodeConfig(args)

  val system: ActorSystem = ActorSystem("Sparxer", config)
  system.spawn[Nothing](guardian, "Guardian")

  logger.info("Engine is up and running. Press [Return] to stop...")
  scala.io.StdIn.readLine()
  system.terminate().onComplete(_ => logger.info("Engine terminated"))

  def guardian: Behavior[Nothing] =
    Actor.deferred[Nothing] { context =>
      val sparkSubmit = new SparkSubmit(setupSpark(config))
      context.spawn(SparkEngine(sparkSubmit.submit), "SparkEngine")
      Actor.ignore
    }

  def setupSpark(config: Config): SparkInstance =
    SparkInstance(
      home = config.getString("engine.spark.home"),
      mode = config.getString("engine.spark.mode"),
      master = config.getString("engine.spark.master"))

  def nodeConfig(args: Array[String]): Config = {
    val port = args.headOption.getOrElse("0")
    ConfigFactory
      .parseString(s"akka.remote.artery.canonical.port = $port")
      .withFallback(ConfigFactory.load())
  }
}
