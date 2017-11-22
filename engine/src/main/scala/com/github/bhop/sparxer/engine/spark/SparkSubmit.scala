package com.github.bhop.sparxer.engine.spark
import java.io.File

import monix.eval.Task
import org.apache.spark.launcher.SparkLauncher
import com.github.bhop.sparxer.protocol.spark.Spark.{JobConfig, JobSubscription, SparkInstance}

class SparkSubmit(spark: SparkInstance) {

  import implicits._

  def submit(config: JobConfig): Task[JobSubscription] =
    for {
      id      <- Task.now(java.util.UUID.randomUUID().toString)
      job     <- buildJob(config, id)
      handler <- Task { job.startApplication() }
    } yield JobSubscription(id, handler.toObservable)

  private def buildJob(config: JobConfig, id: String): Task[SparkLauncher] =
    Task {
      val builder = new SparkLauncher()
        .setSparkHome(spark.home)
        .setMaster(spark.master)
        .setDeployMode(spark.mode)
        .setAppName(config.app.name)
        .setMainClass(config.app.main)
        .setAppResource(config.app.jar)
        .addAppArgs(config.app.args: _*)
        .setVerbose(config.verbose)
        .redirectOutput(new File(s"${config.app.name}-$id.log"))
      spark.props.foldLeft(builder) { case (b, (key, value)) => b.setConf(key, value) }
    }
}

object SparkSubmit {
  def apply(spark: SparkInstance): SparkSubmit = new SparkSubmit(spark)
}
