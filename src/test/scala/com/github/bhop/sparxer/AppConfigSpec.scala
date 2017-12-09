package com.github.bhop.sparxer

import org.scalatest.{Matchers, WordSpec}

class AppConfigSpec extends WordSpec with Matchers {

  "An App Config parser" should {

    import cats.syntax.option._

    "parse engine node together with default node port" in {
      val args = Array("--node-type", "engine")
      AppConfig.parse(args) should be(AppConfig(Engine).some)
    }

    "parse http node together with custom node port" in {
      val args = Array("--node-type", "http", "--port", "2551")
      AppConfig.parse(args) should be(AppConfig(Http, 2551).some)
    }

    "return none if node type is not defined" in {
      val args = Array("--port", "2551")
      AppConfig.parse(args) should be(none[AppConfig])
    }

    "return none if node type is incorrect" in {
      val args = Array("--node-type", "incorrect")
      AppConfig.parse(args) should be(none[AppConfig])
    }
  }
}
