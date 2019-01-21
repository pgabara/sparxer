package sparxer

import com.typesafe.config.{ Config, ConfigFactory }
import pureconfig.generic.auto._
import zio._
import scala.concurrent.duration.FiniteDuration

case class HttpConfig(port: Int, host: String)
case class AuthConfig(secret: String, expirationTime: FiniteDuration)
case class DatabaseConfig(host: String, name: String, user: String, password: String)
case class AppConfig(http: HttpConfig, auth: AuthConfig, database: DatabaseConfig)

object ConfigReader {

  val appConfig: Task[AppConfig] =
    for {
      config    <- ZIO.effect(ConfigFactory.defaultApplication())
      resolved  <- ZIO.effect(config.resolve())
      appConfig <- appConfig(resolved)
    } yield appConfig

  def appConfig(config: Config): Task[AppConfig] =
    Task.effect(pureconfig.loadConfigOrThrow[AppConfig](config))
}
