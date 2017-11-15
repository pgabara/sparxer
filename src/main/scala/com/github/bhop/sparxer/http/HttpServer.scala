package com.github.bhop.sparxer.http

import akka.actor
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.typed.scaladsl.adapter._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import monix.execution.Scheduler

import scala.concurrent.duration._
import com.github.bhop.sparxer.core.Node
import com.github.bhop.sparxer.http.adapters.ClusterSparkEngine

object HttpServer extends App with Node with Routing {

  val config = nodeConfig(args, resourceBasename = "http")

  implicit val system: ActorSystem = ActorSystem("Sparxer", config)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val io: Scheduler = Scheduler.io()
  implicit val timeout: Timeout = Timeout(1.minute)
  implicit val scheduler: actor.Scheduler = system.toTyped.scheduler

  val httpHost = config.getString("http.host")
  val httpPort = config.getInt("http.port")

  val sparkEngineAdapter = system.spawn(ClusterSparkEngine.behavior, "ClusterSparkEngineAdapter")
  val sparkEngine = new ClusterSparkEngine(sparkEngineAdapter)

  val binding = Http().bindAndHandle(routes(sparkEngine), httpHost, httpPort)


  logger.info("Http server is up and running. Press [Return] to stop...")
  scala.io.StdIn.readLine()
  binding.flatMap(_.unbind()).onComplete(_ => system.terminate())
}