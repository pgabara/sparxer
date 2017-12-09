package com.github.bhop.sparxer.http

import akka.actor
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.typed.scaladsl.adapter._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import monix.execution.Scheduler
import com.github.bhop.sparxer.protocol.engine.SparkEngine

import scala.concurrent.duration._

object HttpNode extends Routing with LazyLogging {

  def run(config: Config): Unit = {

    implicit val system: ActorSystem = ActorSystem("Sparxer", config)
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val timeout: Timeout = Timeout(5.minutes)
    implicit val scheduler: actor.Scheduler = system.toTyped.scheduler
    implicit val io: Scheduler = Scheduler.io()

    val sparkEngineRouter = system.spawn(ClusterRouter(SparkEngine.SparkEngineReceptionistKey), "SparkEngineRouter")
    val sparkEngine = ClusterAwareSparkEngineProxy(sparkEngineRouter)

    val http = httpConfig(config)
    val binding = Http().bindAndHandle(routes(sparkEngine), http.host, http.port)

    logger.info("Http server is up and running. Press [Return] to stop...")
    scala.io.StdIn.readLine()
    binding.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }

  case class HttpConfig(host: String, port: Int)

  def httpConfig(config: Config): HttpConfig =
    HttpConfig(
      host = config.getString("http.host"),
      port = config.getInt("http.port")
    )
}
