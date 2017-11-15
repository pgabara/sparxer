package com.github.bhop.sparxer.http

import akka.actor
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.typed.scaladsl.adapter._
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import monix.execution.Scheduler

import com.github.bhop.sparxer.protocol.engine.SparkEngine.SparkEngineReceptionistKey

import scala.concurrent.duration._

object HttpNode extends App with Routing with StrictLogging {

  val config = nodeConfig(args)

  implicit val system: ActorSystem = ActorSystem("Sparxer", config)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout = Timeout(5.minutes)
  implicit val scheduler: actor.Scheduler = system.toTyped.scheduler
  implicit val io: Scheduler = Scheduler.io()

  val sparkEngineRouter = system.spawn(ClusterRouter(SparkEngineReceptionistKey), "SparkEngineRouter")
  val sparkEngine = ClusterAwareSparkEngineProxy(sparkEngineRouter)

  val http = httpConfig(config)
  val binding = Http().bindAndHandle(routes(sparkEngine), http.host, http.port)

  logger.info("Http server is up and running. Press [Return] to stop...")
  scala.io.StdIn.readLine()
  binding.flatMap(_.unbind()).onComplete(_ => system.terminate())

  def nodeConfig(args: Array[String]): Config = {
    val port = args.headOption.getOrElse("0")
    ConfigFactory
      .parseString(s"akka.remote.artery.canonical.port = $port")
      .withFallback(ConfigFactory.load())
  }

  case class HttpConfig(host: String, port: Int)

  def httpConfig(config: Config): HttpConfig =
    HttpConfig(
      host = config.getString("http.host"),
      port = config.getInt("http.port")
    )
}
