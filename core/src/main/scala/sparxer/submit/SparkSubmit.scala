package sparxer.submit

import sparxer.{ Log, SparkJob }
import sparxer.registry.{ SparkJobState, StatusRegistry }
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import zio._
import zio.interop.catz._

object SparkSubmit {
  trait Service[R] {
    def submit(job: SparkJob): ZIO[R, SparkSubmitError, Unit]
  }
}

trait SparkSubmit {
  val sparkSubmit: SparkSubmit.Service[Any]
}

case class SparkSubmitError(reason: String)

trait SparkSubmitLive extends SparkSubmit with StatusRegistry with SparkJobLauncher {

  val sparkSubmit = new SparkSubmit.Service[Any] {

    implicit val unsafeLogger = Slf4jLogger.getLoggerFromName[Task]("SparkSubmitLive")

    def submit(job: SparkJob): ZIO[Any, SparkSubmitError, Unit] =
      (
        for {
          _        <- updateStatusRegistry(job)(SparkJobState.Init("Application registered."))
          statuses <- sparkJobLauncher.start(job).mapError(e => SparkSubmitError(e.reason))
          _        <- statuses.foreach(updateStatusRegistry(job)).fork
        } yield ()
      ).tapError(e => updateStatusRegistry(job)(SparkJobState.Error(e.reason)))

    private def updateStatusRegistry(job: SparkJob)(state: SparkJobState): UIO[Unit] =
      statusRegistry
        .insert(job.id, state)
        .tapBoth(e => Log.error(s"Status registry error: ${e.reason}"), s => Log.debug(s"Updated job status: $s"))
        .ignore
  }
}
