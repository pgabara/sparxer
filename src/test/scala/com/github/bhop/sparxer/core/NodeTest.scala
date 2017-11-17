package com.github.bhop.sparxer.core

import org.scalatest.{Matchers, WordSpec}

class NodeTest extends WordSpec with Matchers with Node {

  "A Node" should {

    "return config with a given port number" in {
      val config = nodeConfig(args = Array("2551"), resourceBasename = "engine")
      config.getString("akka.remote.artery.canonical.port") should equal("2551")
    }

    "return config with a default port number" in {
      val config = nodeConfig(args = Array(), resourceBasename = "http")
      config.getString("akka.remote.artery.canonical.port") should equal("0")
    }
  }
}
