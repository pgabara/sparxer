package sparxer

import cats.Show
import zio._

case class EmailAddress private (value: String) extends AnyVal

object EmailAddress {

  def attempt(raw: String): Option[EmailAddress] = {
    val regex =
      """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
    regex.findFirstMatchIn(raw).map(_ => EmailAddress(raw))
  }

  def unsafe(raw: String): EmailAddress =
    attempt(raw).getOrElse(throw new IllegalArgumentException(s"Invalid format of email: $raw"))
}

case class JobMainClass(value: String) extends AnyVal

object JobMainClass {

  def attempt(raw: String): Option[JobMainClass] =
    Option(JobMainClass(raw))

  def unsafe(raw: String): JobMainClass =
    attempt(raw).getOrElse(throw new IllegalArgumentException(s"Invalid format of main class: $raw"))
}

case class Master(uri: String) extends AnyVal

object Master {

  def attempt(raw: String): Option[Master] =
    Option(Master(raw))

  def unsafe(raw: String): Master =
    attempt(raw).getOrElse(throw new IllegalArgumentException(s"Invalid format of Master: $raw"))
}

sealed trait DeployMode

object DeployMode {
  case object Cluster extends DeployMode
  case object Client  extends DeployMode

  implicit val deployModeShowInstance: Show[DeployMode] =
    Show.show {
      case Cluster => "cluster"
      case Client  => "client"
    }

  def fromString(raw: String): Option[DeployMode] =
    raw match {
      case "cluster" => Option(Cluster)
      case "client"  => Option(Client)
      case _         => Option.empty[DeployMode]
    }
}

case class ApplicationJar(uri: String) extends AnyVal

object ApplicationJar {

  def attempt(raw: String): Option[ApplicationJar] =
    Option(ApplicationJar(raw))

  def unsafe(raw: String): ApplicationJar =
    attempt(raw).getOrElse(throw new IllegalArgumentException(s"Invalid format of application jar: $raw"))
}

case class SparkJobId private (value: Long) extends AnyVal

object SparkJobId {

  val generate: ZIO[Reference, Nothing, SparkJobId] =
    ZIO.accessM[Reference](_.reference.generate).map(SparkJobId(_))

  def unsafeStatic(value: Long): SparkJobId =
    SparkJobId(value)

  implicit val sparkJobIdShowInstance: Show[SparkJobId] =
    Show.show(_.value.toString)
}

case class SparkJob(
  id: SparkJobId,
  mainClass: JobMainClass,
  master: Master,
  deployMode: DeployMode,
  jar: ApplicationJar,
  owner: EmailAddress,
  sparkConf: Map[String, String] = Map.empty,
  args: List[String] = List.empty,
  envs: Map[String, String] = Map.empty
)
