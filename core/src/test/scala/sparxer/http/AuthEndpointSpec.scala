package sparxer.http

import io.circe.generic.auto._
import sparxer.{ AuthConfig, EmailAddress }
import sparxer.registry.{ TestUserRegistry, User, UserRegistry }
import sparxer.http.RouteTesting._
import org.http4s.{ Method, Request, Status, Uri }
import org.specs2.Specification
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import zio._
import zio.clock.Clock
import scala.concurrent.duration._

final class AuthEndpointSpec extends Specification with DefaultRuntime {

  import JsonInstances._

  def is = "Auth Endpoint".title ^ s2"""
    Sig-in user: 
      return Unauthorized when the password is incorrect        $incorrectPassword
      return Unauthorized when the email address is not defined $emailNotDefined
      return valid token                                        $returnToken
    """

  private def incorrectPassword =
    unsafeRun(
      for {
        env      <- environment(List(testUser(password = "test")))
        routes   = (new AuthEndpoint[AuthEnv]).routes(testAuthConfig)
        response <- testRouteResponseR[AuthEnv](routes, signInRequest).provide(env)
      } yield response.status
    ) must be_===(Status.Unauthorized)

  private def emailNotDefined =
    unsafeRun(
      for {
        env      <- environment(List(testUser(email = "test2@test.com")))
        routes   = (new AuthEndpoint[AuthEnv]).routes(testAuthConfig)
        response <- testRouteResponseR[AuthEnv](routes, signInRequest).provide(env)
      } yield response.status
    ) must be_===(Status.Unauthorized)

  private def returnToken =
    unsafeRun(
      for {
        env      <- environment(List(testUser()))
        routes   = (new AuthEndpoint[AuthEnv]).routes(testAuthConfig)
        response <- testRouteResponseR[AuthEnv](routes, signInRequest).provide(env)
      } yield (response.status)
    ) must be_===(Status.Created) // todo: verify token

  private def environment(users: List[User]): UIO[AuthEnv] =
    TestUserRegistry.create(users).map { testUserRegistry =>
      new UserRegistry with Clock.Live {
        val userRegistry = testUserRegistry
      }
    }

  private def testUser(email: String = "test@test.com", password: String = "test123"): User =
    User(EmailAddress.unsafe(email), hashPassword(password))

  private val testAuthConfig: AuthConfig = AuthConfig("test", 1.hour)

  private val signInRequest: Request[TaskR[AuthEnv, ?]] =
    Request[TaskR[AuthEnv, ?]](method = Method.POST, uri = Uri.unsafeFromString("/sign-in"))
      .withEntity(UserCredentials(EmailAddress("test@test.com"), "test123"))

  private def hashPassword(pass: String): PasswordHash[SCrypt] =
    SCrypt.hashpwUnsafe(pass)
}
