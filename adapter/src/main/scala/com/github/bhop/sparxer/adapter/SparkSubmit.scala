package com.github.bhop.sparxer.adapter

import java.io.File

import monix.eval.Task
import org.apache.spark.launcher.SparkLauncher

import domain.{JobConfig, JobSubscription, Spark}

class SparkSubmit(spark: Spark) {

  import implicits._

  def submit(config: JobConfig): Task[JobSubscription] =
    for {
      id      <- uuid()
      job     <- buildJob(config, id)
      handler <- Task { job.startApplication() }
    } yield JobSubscription(id, handler.toObservable)

  private def buildJob(config: JobConfig, id: String): Task[SparkLauncher] =
    Task {
      val builder = new SparkLauncher()
        .setSparkHome(spark.home.toString)
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

  private def uuid(): Task[String] =
    Task { java.util.UUID.randomUUID().toString }
}
