package sparxer.registry

import doobie._
import doobie.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import sparxer.{ EmailAddress, Log }
import tsec.passwordhashers.jca.SCrypt
import tsec.passwordhashers.PasswordHash
import zio._
import zio.interop.catz._

case class User(email: EmailAddress, passwordHash: PasswordHash[SCrypt])

object UserRegistry {
  trait Service[R] {
    def get(email: EmailAddress): ZIO[R, UserRegistryError, Option[User]]
    def insert(user: User): ZIO[R, UserRegistryError, Int]
  }
}

trait UserRegistry {
  val userRegistry: UserRegistry.Service[Any]
}

case class UserRegistryError(reason: String)

object UserRegistryError {
  def apply(t: Throwable): UserRegistryError = UserRegistryError(t.getMessage)
}

trait DoobieUserRegistry extends UserRegistry {

  protected def transactor: Transactor[Task]

  val userRegistry = new UserRegistry.Service[Any] {

    import UserRegistryStatements._

    implicit val unsafeLogger = Slf4jLogger.getLoggerFromName[Task]("DoobieUserRegistry")

    def get(email: EmailAddress): ZIO[Any, UserRegistryError, Option[User]] =
      getUserByEmail(email).option
        .transact(transactor)
        .tapError(e => Log.error(e, "Cannot get user from a database."))
        .mapError(UserRegistryError(_))

    def insert(user: User): ZIO[Any, UserRegistryError, Int] =
      insertUser(user).run
        .transact(transactor)
        .tapError(e => Log.error(e, "Cannot insert new user."))
        .mapError(UserRegistryError(_))
  }
}

object UserRegistryStatements {

  import sparxer.Database.DoobieMetaInstances._

  def getUserByEmail(email: EmailAddress): Query0[User] =
    sql"SELECT * FROM users WHERE email = $email".query[User]

  def insertUser(user: User): Update0 =
    Update[User]("INSERT INTO users VALUES (?, ?)").toUpdate0(user)
}
