package sparxer
import org.specs2.Specification
import zio.DefaultRuntime
import zio.Task
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

final class ConfigReaderSpec extends Specification with DefaultRuntime {

  def is = "Config Reader".title ^ s2"""
    Read app config:
      parse and return app config $read
    """

  private def read = {
    val program = for {
      config    <- testConfig
      appConfig <- ConfigReader.appConfig(config)
    } yield appConfig
    unsafeRun(program.either) must be_===(Right(appConfig))
  }

  private val appConfig: AppConfig =
    AppConfig(
      http = HttpConfig(9000, "0.0.0.0"),
      auth = AuthConfig("secret", 1.hour),
      database = DatabaseConfig("0.0.0.0", "sparxer_test", "sparxer_test", "")
    )

  private val testConfig: Task[Config] = Task.effect(ConfigFactory.load("test.application"))
}
