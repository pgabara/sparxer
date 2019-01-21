package sparxer.http

import cats.data.OptionT
import cats.data.Kleisli
import cats.syntax.either._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Printer
import sparxer.{ AuthConfig, EmailAddress, Log }
import sparxer.http.Auth.{ AuthError, UserData }
import tsec.jwt.JWTClaims
import tsec.jws.mac.JWTMac
import tsec.mac.jca.{ HMACSHA256, MacSigningKey }
import tsec.common.VerificationStatus
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import org.http4s.{ AuthScheme, AuthedRoutes, Request }
import org.http4s.headers.Authorization
import org.http4s.Credentials.Token
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import zio._
import zio.clock._
import zio.interop.catz._

import java.util.concurrent.TimeUnit
import java.time.Instant

object Auth {

  implicit val unsafeLogger = Slf4jLogger.getLoggerFromName[Task]("Auth")

  case class UserData(email: EmailAddress)

  case class AuthError(reason: String)

  def hashPassword(password: String): IO[AuthError, PasswordHash[SCrypt]] =
    SCrypt.hashpw[Task](password).mapError(e => AuthError(s"Cannot hash password: ${e.getMessage}"))

  def verifyPassword(password: String, passwordHash: PasswordHash[SCrypt]): IO[AuthError, VerificationStatus] =
    SCrypt.checkpw[Task](password, passwordHash).mapError(e => AuthError(s"Cannot verify password: ${e.getMessage}"))

  def verifyToken(config: AuthConfig)(token: String): IO[AuthError, UserData] =
    for {
      key         <- signingKey(config)
      parsedToken <- verifyAndParse(token, key)
      userData    <- decodeUserData(parsedToken)
    } yield userData

  def generateToken(
    user: UserData,
    config: AuthConfig
  ): ZIO[Clock, AuthError, String] = {
    import io.circe.syntax._
    import io.circe.generic.auto._
    for {
      key           <- signingKey(config)
      json          = Printer.noSpaces.pretty(user.asJson)
      expiry        <- currentTime(TimeUnit.MILLISECONDS).map(m => config.expirationTime.toMillis + m)
      expiryInstant = Instant.ofEpochMilli(expiry)
      claims        = JWTClaims(expiration = Option(expiryInstant), subject = Option(json))
      token         <- generateJWTToken(claims, key)
    } yield token
  }

  private def signingKey(config: AuthConfig): IO[AuthError, MacSigningKey[HMACSHA256]] =
    HMACSHA256
      .buildKey[Task](config.secret.getBytes)
      .tapError(e => Log.warn(s"Cannot generate signing key: ${e.getMessage}"))
      .mapError(_ => AuthError("Cannot generate signing key."))

  private def verifyAndParse(token: String, signingKey: MacSigningKey[HMACSHA256]): IO[AuthError, JWTMac[HMACSHA256]] =
    JWTMac
      .verifyAndParse[Task, HMACSHA256](token, signingKey)
      .tapError(e => Log.warn(s"Cannot parse given token: ${e.getMessage}"))
      .mapError(_ => AuthError("Cannot parse given token."))

  private def decodeUserData(jwt: JWTMac[HMACSHA256]): IO[AuthError, UserData] = {
    import io.circe.parser.decode
    import io.circe.generic.auto._
    IO.fromOption(jwt.body.subject)
      .flatMap(subject => IO.fromEither(decode[UserData](subject)))
      .mapError(_ => AuthError("Invalid token."))
  }

  private def generateJWTToken(claims: JWTClaims, key: MacSigningKey[HMACSHA256]): IO[AuthError, String] =
    JWTMac
      .buildToString[Task, HMACSHA256](claims, key)
      .tapError(e => Log.warn(s"Cannot generate new token: ${e.getMessage}"))
      .mapError(e => AuthError("Cannot generate new token."))
}

final class JWTAuthException(error: AuthError) extends Exception(error.reason)

class JWTAuthMiddleware[R](config: AuthConfig) extends Http4sDsl[TaskR[R, ?]] {

  def middleware: AuthMiddleware[TaskR[R, ?], UserData] =
    AuthMiddleware(authorize, onFailure)

  private val authorize: Kleisli[TaskR[R, ?], Request[TaskR[R, ?]], Either[String, UserData]] =
    Kleisli { request =>
      val userData = for {
        token    <- extractBearerToken(request)
        userData <- Auth.verifyToken(config)(token)
      } yield userData
      userData.either.flatMap {
        case Right(user) => Task.succeed(user.asRight)
        case Left(error) => Task.succeed(error.reason.asLeft)
      }
    }

  private val onFailure: AuthedRoutes[String, TaskR[R, ?]] =
    Kleisli { request =>
      import io.circe.generic.auto._
      import JsonInstances._
      OptionT.liftF(Forbidden(ErrorResponse(request.authInfo)))
    }

  private def extractBearerToken(request: Request[TaskR[R, ?]]): IO[AuthError, String] =
    request.headers.get(Authorization) match {
      case Some(Authorization(Token(AuthScheme.Bearer, token))) => IO.succeed(token)
      case _                                                    => IO.fail(AuthError("Authorization header not provided or invalid."))
    }
}

object JWTAuthMiddleware {
  def middleware[R](config: AuthConfig): AuthMiddleware[TaskR[R, ?], UserData] =
    new JWTAuthMiddleware[R](config).middleware
}
