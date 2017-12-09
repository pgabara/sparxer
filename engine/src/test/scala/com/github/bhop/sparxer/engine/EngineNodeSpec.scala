package com.github.bhop.sparxer.engine

import com.github.bhop.sparxer.protocol.spark.Spark.SparkInstance
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpec}

class EngineNodeSpec extends WordSpec with Matchers {

  import EngineNode._

  "An Engine Node" should {

    "read spark instance from configuration" in {
      setupSpark(ConfigFactory.load()) should be(SparkInstance(home = "/opt/spark", master = "local[*]"))
    }
  }
}
