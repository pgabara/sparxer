package com.github.bhop.sparxer

import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpec}

class RunnerSpec extends WordSpec with Matchers {

  "A Runner" should {

    "throw error if provided node type is unknown" in {
      intercept[IllegalStateException] {
        Runner.interpret(AppConfig(), ConfigFactory.load())
      }
    }
  }
}
