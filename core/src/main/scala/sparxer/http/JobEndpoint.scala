package sparxer.http

import io.circe.generic.auto._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl
import sparxer.SparkJobId
import zio._
import zio.interop.catz._

final class JobEndpoint[R <: JobEnv] extends Http4sDsl[TaskR[R, ?]] {

  type JobIO[A] = TaskR[R, A]

  implicit val unsafeLogger = Slf4jLogger.getLoggerFromName[Task]("JobEndpoint")

  import JsonInstances._

  val routes: AuthedRoutes[Auth.UserData, JobIO] =
    AuthedRoutes.of {
      case authReq @ GET -> Root as user =>
        for {
          env      <- ZIO.environment[JobEnv]
          userJobs <- env.jobRegistry.getUserJobs(user.email).mapError(toException)
          response <- Ok(userJobs)
        } yield response

      case authReq @ GET -> Root / LongVar(id) as _ =>
        for {
          env         <- ZIO.environment[JobEnv]
          sparkJobOpt <- env.jobRegistry.get(SparkJobId.unsafeStatic(id)).mapError(toException)
          response <- sparkJobOpt match {
                       case None => NotFound(ErrorResponse(s"Spark job with id: $id not found."))
                       case Some(sparkJob) =>
                         env.statusRegistry
                           .get(sparkJob.id)
                           .mapError(toException)
                           .flatMap(statuses => Ok(GetSparkJobDetails(sparkJob, statuses)))
                     }
        } yield response
    }
}
