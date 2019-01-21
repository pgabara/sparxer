package sparxer.registry

import doobie._
import doobie.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import sparxer.{ Log, SparkJobId }
import zio._
import zio.clock._
import zio.interop.catz._

import java.time.Instant
import java.util.concurrent.TimeUnit

sealed trait SparkJobState

object SparkJobState {
  case class Init(description: String)       extends SparkJobState
  case class InProgress(description: String) extends SparkJobState
  case class Completed(description: String)  extends SparkJobState
  case class Error(reason: String)           extends SparkJobState
}

case class Status(jobId: SparkJobId, state: SparkJobState, time: Instant)

object Status {

  def create(jobId: SparkJobId, state: SparkJobState, millis: Long): Status =
    Status(jobId, state, Instant.ofEpochMilli(millis))
}

case class StatusRegistryError(reason: String)

object StatusRegistryError {
  def apply(t: Throwable): StatusRegistryError = StatusRegistryError(t.getMessage)
}

object StatusRegistry {
  trait Service[R] {
    def insert(jobId: SparkJobId, state: SparkJobState): ZIO[R, StatusRegistryError, Status]
    def get(jobId: SparkJobId): ZIO[R, StatusRegistryError, List[Status]]
  }
}

trait StatusRegistry {
  val statusRegistry: StatusRegistry.Service[Any]
}

trait DoobieStatusRegistry extends StatusRegistry with Clock {

  protected def transactor: Transactor[Task]

  val statusRegistry = new StatusRegistry.Service[Any] {

    import StatusRegistryStatements._

    implicit val unsafeLogger = Slf4jLogger.getLoggerFromName[Task]("DoobieStatusRegistry")

    def get(jobId: SparkJobId): ZIO[Any, StatusRegistryError, List[Status]] =
      getJobStatuses(jobId)
        .to[List]
        .transact(transactor)
        .tapError(e => Log.error(e, "Cannot get job statuses due to a database error."))
        .mapError(StatusRegistryError(_))

    def insert(jobId: SparkJobId, state: SparkJobState): ZIO[Any, StatusRegistryError, Status] =
      for {
        millis <- clock.currentTime(TimeUnit.MILLISECONDS)
        status = Status.create(jobId, state, millis)
        _ <- insertOne(status).run
              .transact(transactor)
              .tapError(e => Log.error(e, "Cannot insert new status due to a database error."))
              .mapError(StatusRegistryError(_))
      } yield status
  }
}

object StatusRegistryStatements {

  import sparxer.Database.DoobieMetaInstances._

  def insertOne(status: Status): Update0 =
    Update[Status]("INSERT INTO statuses (job_id, state, time) VALUES (?, ?, ?)").toUpdate0(status)

  def getJobStatuses(id: SparkJobId): Query0[Status] =
    sql"SELECT job_id, state, time FROM statuses WHERE job_id = $id".query[Status]
}
