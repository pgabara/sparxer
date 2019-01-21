package sparxer.http

import io.circe.generic.auto._
import org.http4s.{ HttpRoutes, Response }
import org.http4s.dsl.Http4sDsl
import sparxer.{ AuthConfig, EmailAddress }
import sparxer.http.Auth.UserData
import sparxer.registry.User
import tsec.common.{ VerificationFailed, VerificationStatus, Verified }
import zio._
import zio.interop.catz._

class AuthEndpoint[R <: AuthEnv] extends Http4sDsl[TaskR[R, ?]] {

  type AuthIO[A] = TaskR[R, A]

  import JsonInstances._

  def routes(config: AuthConfig): HttpRoutes[AuthIO] =
    HttpRoutes.of {
      case req @ POST -> Root / "sign-in" =>
        for {
          env          <- ZIO.environment[AuthEnv]
          credentials  <- req.as[UserCredentials]
          verification <- verifyUserCredentials(credentials)
          response <- verification match {
                       case Verified =>
                         generateToken(credentials.email, config).flatMap(token => Created(UserToken(token)))
                       case VerificationFailed => ZIO.succeed(Response[AuthIO](Unauthorized))
                     }
        } yield response

      case req @ POST -> Root / "sign-up" => // todo: dummy version. email verification/confirmation needs to be added.
        for {
          env          <- ZIO.environment[AuthEnv]
          credentials  <- req.as[UserCredentials]
          passwordHash <- Auth.hashPassword(credentials.password).mapError(toException)
          _            <- env.userRegistry.insert(User(credentials.email, passwordHash)).mapError(toException)
          response     <- Created()
        } yield response
    }

  private def generateToken(email: EmailAddress, config: AuthConfig): TaskR[AuthEnv, String] =
    Auth.generateToken(UserData(email), config).mapError(toException)

  private def verifyUserCredentials(credentials: UserCredentials): TaskR[AuthEnv, VerificationStatus] =
    ZIO
      .accessM[AuthEnv](_.userRegistry.get(credentials.email).mapError(toException))
      .flatMap {
        case Some(user) => Auth.verifyPassword(credentials.password, user.passwordHash).mapError(toException)
        case None       => ZIO.succeed(VerificationFailed)
      }
}
