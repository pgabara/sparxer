package com.github.bhop.sparxer.core

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging

trait Node extends StrictLogging {

  def nodeConfig(args: Array[String], resourceBasename: String): Config =
    ConfigFactory
      .parseString(s"akka.remote.artery.canonical.port = ${port(args)}")
      .withFallback(ConfigFactory.load(resourceBasename + ".cluster"))

  private def port(args: Array[String]): String = args.headOption.getOrElse("0")
}
