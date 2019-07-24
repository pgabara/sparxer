package sparxer.http

import org.specs2.Specification
import sparxer.{ AuthConfig, EmailAddress }
import tsec.common.Verified
import scala.concurrent.duration._
import tsec.common.VerificationFailed
import zio._

final class AuthSpec extends Specification with DefaultRuntime {

  def is = "Auth".title ^ s2"""
    Generate and verify tokens:
      use the same secret         $generateAndVerify
      use different secrets       $generateAndVerifyDifferentSecrets
      use expired token           $verifyExpiredToken
    Hash and verify passwords:
      use the same passwords       $hashAndVerifyCorrectPassword
      use different passwords      $hashAndVerifyIncorrectPassword
    """

  import Auth._

  private def generateAndVerify = {
    val config = AuthConfig("test-secret", 1.hour)
    unsafeRun(
      for {
        token <- generateToken(UserData(EmailAddress.unsafe("test@test.com")), config)
        user  <- verifyToken(config)(token)
      } yield user
    ) must be_===(UserData(EmailAddress.unsafe("test@test.com")))
  }

  private def generateAndVerifyDifferentSecrets = {
    val config1 = AuthConfig("test-secret-1", 1.hour)
    val config2 = AuthConfig("test-secret-2", 1.hour)
    val program = for {
      token <- generateToken(UserData(EmailAddress.unsafe("test@test.com")), config1)
      user  <- verifyToken(config2)(token)
    } yield user
    unsafeRun(program.either) must be_===(Left(AuthError("Cannot parse given token.")))
  }

  private def verifyExpiredToken = {
    val config = AuthConfig("secret", 0.seconds)
    val program = for {
      token <- generateToken(UserData(EmailAddress.unsafe("test@test.com")), config)
      user  <- verifyToken(config)(token)
    } yield user
    unsafeRun(program.either) must be_===(Left(AuthError("Cannot parse given token.")))
  }

  private def hashAndVerifyCorrectPassword = {
    val program = for {
      p <- hashPassword("test123")
      v <- verifyPassword("test123", p)
    } yield v
    unsafeRun(program.either) must be_===(Right(Verified))
  }

  private def hashAndVerifyIncorrectPassword = {
    val program = for {
      p <- hashPassword("test123")
      v <- verifyPassword("TEST_123", p)
    } yield v
    unsafeRun(program.either) must be_===(Right(VerificationFailed))
  }
}
