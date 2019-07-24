package sparxer.submit

import cats.syntax.show._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.apache.spark.launcher.{ SparkAppHandle, SparkLauncher }
import sparxer.{ Log, SparkJob }
import sparxer.registry.SparkJobState
import zio._
import zio.stream._
import zio.interop.catz._
import scala.collection.JavaConverters._

case class SparkLauncherError(reason: String)

object SparkJobLauncher {
  trait Service[R] {
    def start(job: SparkJob): ZIO[R, SparkLauncherError, Stream[Nothing, SparkJobState]]
  }
}

trait SparkJobLauncher {
  val sparkJobLauncher: SparkJobLauncher.Service[Any]
}

trait SparkJobLauncherLive extends SparkJobLauncher {

  val sparkJobLauncher = new SparkJobLauncher.Service[Any] {

    implicit val unsafeLogger = Slf4jLogger.getLoggerFromName[Task]("SparkJobLauncher")

    def start(job: SparkJob): ZIO[Any, SparkLauncherError, Stream[Nothing, SparkJobState]] =
      for {
        spark    <- IO.succeed(createSparkLauncher(job))
        handler  <- startSparkJob(spark)
        statuses = sparkJobStatuses(handler)
      } yield statuses

    private def createSparkLauncher(job: SparkJob): SparkLauncher = {
      val sparkLauncher = new SparkLauncher(job.envs.asJava)
        .setAppName(job.id.show)
        .setMaster(job.master.uri)
        .setDeployMode(job.deployMode.show)
        .setMainClass(job.mainClass.value)
        .setAppResource(job.jar.uri)
        .addAppArgs(job.args: _*)
      job.sparkConf.foldLeft(sparkLauncher) { case (s, (k, v)) => s.setConf(k, v) }
    }

    private def startSparkJob(spark: SparkLauncher): IO[SparkLauncherError, SparkAppHandle] =
      IO.effect(spark.startApplication())
        .tapError(Log.error(_, "Cannot submit new spark job."))
        .mapError(e => SparkLauncherError(e.getMessage))
  }
}
