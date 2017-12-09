package com.github.bhop.sparxer

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import com.github.bhop.sparxer.engine.EngineNode
import com.github.bhop.sparxer.http.HttpNode

object Runner extends App with LazyLogging {

  for {
    app <- AppConfig.parse(args)
    _ = logger.info(s"Running node [${app.node}] on port: ${app.port}")
    config = ConfigReader.nodeConfig(app)
  } yield interpret(app, config)

  def interpret(app: AppConfig, config: Config): Unit =
    app.node match {
      case Engine => EngineNode.run(config)
      case Http => HttpNode.run(config)
      case Unspecified => throw new IllegalStateException("Cannot parse node configuration")
    }
}
