package com.github.bhop.sparxer

import scopt.OptionParser

sealed trait NodeType
case object Unspecified extends NodeType
case object Engine extends NodeType
case object Http extends NodeType

case class AppConfig(node: NodeType = Unspecified, port: Long = 0)

object AppConfig {

  def parse(args: Array[String]): Option[AppConfig] =
    parser.parse(args, AppConfig())

  private def parser: OptionParser[AppConfig] = new OptionParser[AppConfig]("sparxer") {

    implicit val nodeTypeRead: scopt.Read[NodeType] =
      scopt.Read.reads {
        case "engine" => Engine
        case "http"   => Http
        case _        => Unspecified
      }

    head("sparxer", "0.0.1")

    opt[NodeType]('n', "node-type").required()
      .action((value, config) => config.copy(node = value))
      .text("cluster node type: [engine|http]")
      .validate {
        case Unspecified => failure("Cannot recognize provided node type")
        case _ => success
      }

    opt[Long]('p', "port").optional()
      .action((value, config) => config.copy(port = value))
      .text("node port number e.g. 2551")

    help("help").text("prints this usage text")
  }
}
