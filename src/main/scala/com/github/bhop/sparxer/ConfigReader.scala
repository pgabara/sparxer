package com.github.bhop.sparxer

import com.typesafe.config.{Config, ConfigFactory}

object ConfigReader {

  def nodeConfig(app: AppConfig): Config = {
    ConfigFactory
        .parseString(
          s"""
             |{
             |  akka {
             |    remote.artery.canonical.port = ${app.port}
             |    actor.cluster.roles = [${nodeTypeToClusterRole(app.node)}]
             |  }
             |}
           """.stripMargin)
      .withFallback(ConfigFactory.load())
  }

  private def nodeTypeToClusterRole(nodeType: NodeType): String =
    nodeType match {
      case Engine => "engine"
      case Http => "http"
      case Unspecified => ""
    }
}
