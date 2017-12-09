package com.github.bhop.sparxer

import org.scalatest.{Matchers, WordSpec}
import scala.collection.JavaConverters._

class ConfigReaderSpec extends WordSpec with Matchers {

  "A Config Reader" should {

    "propagate node port number and node role" in {
      val config = ConfigReader.nodeConfig(AppConfig(Engine, 2551))
      config.getStringList("akka.actor.cluster.roles").asScala.head should be("engine")
      config.getLong("akka.remote.artery.canonical.port") should be(2551)
    }

    "propagate empty cluster roles if node type is unspecified and default node port number" in {
      val config = ConfigReader.nodeConfig(AppConfig(Unspecified))
      config.getStringList("akka.actor.cluster.roles").asScala.size should be(0)
      config.getLong("akka.remote.artery.canonical.port") should be(0)
    }
  }
}
