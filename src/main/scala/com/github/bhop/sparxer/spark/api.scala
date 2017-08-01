package com.github.bhop.sparxer.spark

import java.io.File

import monix.eval.Task
import org.apache.spark.launcher.{SparkAppHandle, SparkLauncher}

object api {

  import domain._

  class SparkStateHandler(raw: SparkAppHandle) extends StateHandler {

    def state: State = State(name = raw.getState.name(), isFinal = raw.getState.isFinal)

    def disconnect(): Task[State] =
      Task.eval(raw.disconnect()).map(_ => state)

    def onChange(f: (State) => Unit): StateHandler = {
      raw.addListener(new SparkAppHandle.Listener {
        def infoChanged(h: SparkAppHandle): Unit = ()
        def stateChanged(h: SparkAppHandle): Unit = f(State(h.getState.name(), h.getState.isFinal))
      })
      this
    }
  }

  def submitViaSparkLauncher(spark: Spark, app: App): Task[Submission] =
    uuid().map { id =>
      val baseSparkConfig = new SparkLauncher()
        .setAppName(app.name)
        .setAppResource(app.jar)
        .addAppArgs(app.args: _*)
        .setMainClass(app.main)
        .setMaster(spark.master)
        .setSparkHome(spark.home)
        .setDeployMode(spark.mode)
        .redirectOutput(new File(s"${app.name}-$id.log"))
        .setVerbose(true)

      val sparkConfig = spark.props.foldLeft(baseSparkConfig) { (c, p) => c.setConf(p._1, p._2) }
      Submission(id, handler = new SparkStateHandler(sparkConfig.startApplication()))
    }

  private def uuid(): Task[String] =
    Task.eval(java.util.UUID.randomUUID().toString)
}
