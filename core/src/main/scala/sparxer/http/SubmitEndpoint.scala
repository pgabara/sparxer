package sparxer.http

import io.circe.generic.auto._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import sparxer._
import sparxer.http.Auth.UserData
import sparxer.submit.SparkSubmitError
import org.http4s.{ AuthedRoutes, Response }
import org.http4s.dsl.Http4sDsl
import zio._
import zio.interop.catz._

class SubmitEndpoint[R <: SubmitEnv] extends Http4sDsl[TaskR[R, ?]] {

  type SubmitIO[A] = TaskR[R, A]

  implicit val unsafeLogger = Slf4jLogger.getLoggerFromName[Task]("SubmitEndpoint")

  import JsonInstances._

  val routes: AuthedRoutes[UserData, SubmitIO] =
    AuthedRoutes.of {
      case authReq @ POST -> Root / "submit" as user =>
        for {
          env      <- ZIO.environment[SubmitEnv]
          json     <- authReq.req.as[SubmitSparkJob]
          sparkJob <- createSparkJob(json, user)
          _        <- Log.debug(s"Submitting new job: $sparkJob")
          _        <- env.jobRegistry.register(sparkJob).mapError(toException)
          response <- sparkSubmitToResponse(sparkJob.id, env.sparkSubmit.submit(sparkJob))
        } yield response

      case authReq @ POST -> Root / "resubmit" as user =>
        for {
          env    <- ZIO.environment[SubmitEnv]
          json   <- authReq.req.as[ResubmitSparkJob]
          jobOpt <- env.jobRegistry.get(json.id).mapError(toException)
          response <- jobOpt match {
                       case Some(sparkJob) =>
                         for {
                           newJobId <- SparkJobId.generate
                           newJob   = sparkJob.copy(id = newJobId, owner = user.email)
                           _        <- Log.debug(s"Resubmitting job with id: ${json.id}: $newJob")
                           _        <- env.jobRegistry.register(newJob).mapError(toException)
                           resp     <- sparkSubmitToResponse(newJobId, env.sparkSubmit.submit(newJob))
                         } yield resp
                       case None => NotFound(ErrorResponse(s"Job with id: ${json.id.value} does not exist."))
                     }
        } yield response
    }

  private def sparkSubmitToResponse(
    id: SparkJobId,
    submit: ZIO[Any, SparkSubmitError, Unit]
  ): SubmitIO[Response[SubmitIO]] =
    submit.either.flatMap {
      case Right(_) => Created(SubmittedSparkJob(id))
      case Left(e)  => InternalServerError(ErrorResponse(e.reason))
    }

  private def createSparkJob(json: SubmitSparkJob, user: UserData): ZIO[Reference, Nothing, SparkJob] =
    SparkJobId.generate.map { id =>
      SparkJob(
        id = id,
        mainClass = json.mainClass,
        master = json.master,
        deployMode = json.deployMode,
        jar = json.jar,
        owner = user.email,
        sparkConf = json.sparkConf,
        args = json.args,
        envs = json.envs
      )
    }
}
