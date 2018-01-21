package com.github.bhop.sparxer.engine.spark

import com.typesafe.config.ConfigFactory
import com.github.bhop.sparxer.protocol.spark.Spark._

trait SparkOps {

  private val config = ConfigFactory.load()

  def testSparkInstance: SparkInstance =
    SparkInstance(
      home = config.getString("test.spark.home"),
      master = "local[*]"
    )

  import scala.collection.JavaConverters._

  def testJobConfig: JobConfig =
    JobConfig(
      SparkApp(
        name = "test-app",
        jar = config.getString("test.spark.app.jar"),
        main = config.getString("test.spark.app.main"),
        args = config.getStringList("test.spark.app.args").asScala.toList
      )
    )
}
