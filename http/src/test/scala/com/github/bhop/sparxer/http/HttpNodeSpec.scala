package com.github.bhop.sparxer.http

import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpec}

class HttpNodeSpec extends WordSpec with Matchers {

  import HttpNode._

  "A Http Node" should {

    "read http config" in {
      httpConfig(ConfigFactory.load()) should be(HttpConfig(host = "localhost", port = 9000))
    }

    "set default node port" in {
      nodeConfig(Array.empty).getString("akka.remote.artery.canonical.port") should be ("0")
    }

    "set custom node port" in {
      nodeConfig(Array("2551")).getString("akka.remote.artery.canonical.port") should be("2551")
    }
  }
}
