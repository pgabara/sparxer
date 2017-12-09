package com.github.bhop.sparxer.http

import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpec}

class HttpNodeSpec extends WordSpec with Matchers {

  import HttpNode._

  "A Http Node" should {

    "read http config" in {
      httpConfig(ConfigFactory.load()) should be(HttpConfig(host = "localhost", port = 9000))
    }
  }
}
