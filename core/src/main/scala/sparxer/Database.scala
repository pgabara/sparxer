package sparxer

import cats.syntax.show._
import doobie._
import doobie.hikari.HikariTransactor
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import sparxer.registry.SparkJobState
import zio._
import zio.interop.catz._
import scala.concurrent.ExecutionContext

object Database {

  def hikariTransactor[R](config: DatabaseConfig, connectionEC: ExecutionContext, transactionEC: ExecutionContext)(
    implicit rts: Runtime[R]
  ): Managed[Throwable, Transactor[Task]] =
    HikariTransactor
      .newHikariTransactor[Task](
        "org.postgresql.Driver",
        s"jdbc:postgresql://${config.host}/${config.name}",
        config.user,
        config.password,
        connectionEC,
        transactionEC
      )
      .toManaged

  case class DatabaseError(reason: String)

  object DoobieMetaInstances {

    implicit val sparkJobIdMetaInstance: Meta[SparkJobId] =
      Meta[Long].timap(SparkJobId.unsafeStatic)(_.value)

    implicit val sparkJobStateMetaInstance: Meta[SparkJobState] =
      SparkJobStateMetaInstance.apply

    private object SparkJobStateMetaInstance {
      import io.circe.syntax._
      import io.circe.parser.decode
      import io.circe.generic.extras.auto._
      import io.circe.generic.extras.Configuration
      implicit val c = Configuration.default.withDiscriminator("stateType")
      private def fromJson(json: String) =
        decode[SparkJobState](json).getOrElse(throw new IllegalArgumentException(s"Invalid spark job state: $json"))
      def apply: Meta[SparkJobState] = Meta[String].timap(fromJson)(_.asJson.noSpaces)
    }

    implicit val jobMainClassMetaInstance: Meta[JobMainClass] =
      Meta[String].timap(JobMainClass.unsafe)(_.value)

    implicit val masterMetaInstance: Meta[Master] =
      Meta[String].timap(Master.unsafe)(_.uri)

    implicit val deployModeMetaInstance: Meta[DeployMode] =
      Meta[String].timap(DeployMode.fromString(_).getOrElse(DeployMode.Client))(_.show)

    implicit val applicationJarMetaInstance: Meta[ApplicationJar] =
      Meta[String].timap(ApplicationJar.unsafe)(_.uri)

    implicit val passwordHashMetaInstance: Meta[PasswordHash[SCrypt]] =
      implicitly[Meta[String]].asInstanceOf[Meta[PasswordHash[SCrypt]]]
  }
}
