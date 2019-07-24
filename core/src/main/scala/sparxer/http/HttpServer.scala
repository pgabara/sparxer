package sparxer.http

import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.server.Router
import org.http4s.syntax.kleisli._
import sparxer.{ AppConfig, Log }
import zio._
import zio.interop.catz._

object HttpServer {

  type HttpIO[A] = TaskR[HttpEnv, A]

  implicit val unsafeLogger = Slf4jLogger.getLoggerFromName[Task]("HttpServer")

  def start(config: AppConfig): TaskR[HttpEnv, Unit] =
    TaskR.runtime[HttpEnv].flatMap { implicit rts =>
      for {
        _ <- BlazeServerBuilder[HttpIO]
              .bindHttp(config.http.port, config.http.host)
              .withHttpApp(httpApp(config))
              .serve
              .compile
              .drain
        _ <- Log.debug(s"Server is up and running on port ${config.http.port}.")
      } yield ()
    }

  private def httpApp(config: AppConfig): HttpApp[HttpIO] = {
    val auth = JWTAuthMiddleware.middleware[HttpEnv](config.auth)
    Logger.httpApp(logHeaders = true, logBody = true)(
      Router[HttpIO](
        "/spark"  -> auth(new SubmitEndpoint[HttpEnv].routes),
        "/jobs"   -> auth(new JobEndpoint[HttpEnv].routes),
        "/auth"   -> new AuthEndpoint[HttpEnv].routes(config.auth),
        "/health" -> new HealthCheckEndpoint[HttpEnv].routes
      ).orNotFound
    )
  }
}
