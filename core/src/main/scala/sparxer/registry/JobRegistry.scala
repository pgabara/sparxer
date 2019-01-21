package sparxer.registry

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import sparxer.{ EmailAddress, Log, SparkJob, SparkJobId }
import zio._
import zio.interop.catz._

object JobRegistry {
  trait Service[R] {
    def register(job: SparkJob): ZIO[R, JobRegistryError, SparkJob]
    def get(id: SparkJobId): ZIO[R, JobRegistryError, Option[SparkJob]]
    def getUserJobs(user: EmailAddress): ZIO[R, JobRegistryError, List[SparkJob]]
  }
}

trait JobRegistry {
  val jobRegistry: JobRegistry.Service[Any]
}

case class JobRegistryError(reason: String)

object JobRegistryError {
  def apply(t: Throwable): JobRegistryError = JobRegistryError(t.getMessage)
}

trait DoobieJobRegistry extends JobRegistry {

  protected def transactor: Transactor[Task]

  val jobRegistry = new JobRegistry.Service[Any] {

    import JobRegistryStatements._

    implicit val unsafeLogger = Slf4jLogger.getLoggerFromName[Task]("DoobieJobRegistry")

    def get(id: SparkJobId): ZIO[Any, JobRegistryError, Option[SparkJob]] =
      getOne(id).option
        .transact(transactor)
        .tapError(e => Log.error(e, "Cannot get job due to a database error."))
        .mapError(JobRegistryError(_))

    def getUserJobs(user: EmailAddress): ZIO[Any, JobRegistryError, List[SparkJob]] =
      getByUser(user)
        .to[List]
        .transact(transactor)
        .tapError(e => Log.error(e, "Cannot get user jobs due to a database error."))
        .mapError(JobRegistryError(_))

    def register(job: SparkJob): ZIO[Any, JobRegistryError, SparkJob] =
      insertOne(job).run
        .transact(transactor)
        .tapError(e => Log.error(e, "Cannot insert new job due to a database error."))
        .mapError(JobRegistryError(_)) *> ZIO.succeed(job)
  }
}

object JobRegistryStatements {

  import sparxer.Database.DoobieMetaInstances._

  def insertOne(job: SparkJob): Update0 =
    Update[SparkJob]("INSERT INTO jobs VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)").toUpdate0(job)

  def getOne(jobId: SparkJobId): Query0[SparkJob] =
    sql"SELECT * FROM jobs WHERE id = $jobId".query[SparkJob]

  def getByUser(owner: EmailAddress): Query0[SparkJob] =
    sql"SELECT * FROM jobs WHERE owner = $owner".query[SparkJob]
}
