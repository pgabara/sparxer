package com.github.bhop.sparxer

import org.scalatest.{Matchers, WordSpec}
import scala.collection.JavaConverters._

class ConfigReaderSpec extends WordSpec with Matchers {

  "A Config Reader" should {

    "propagate node port number and node role" in {
      val engineConfig = ConfigReader.nodeConfig(AppConfig(Engine, 2551))
      engineConfig.getStringList("akka.actor.cluster.roles").asScala.head should be("engine")
      engineConfig.getLong("akka.remote.artery.canonical.port") should be(2551)
      val httpConfig = ConfigReader.nodeConfig(AppConfig(Http, 2551))
      httpConfig.getStringList("akka.actor.cluster.roles").asScala.head should be("http")
    }

    "propagate empty cluster roles if node type is unspecified and default node port number" in {
      val config = ConfigReader.nodeConfig(AppConfig(Unspecified))
      config.getStringList("akka.actor.cluster.roles").asScala.size should be(0)
      config.getLong("akka.remote.artery.canonical.port") should be(0)
    }
  }
}
