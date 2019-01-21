package sparxer.http

import cats.syntax.show._
import io.circe.syntax._
import io.circe.{ Decoder, Encoder }
import org.http4s.{ EntityDecoder, EntityEncoder }
import org.http4s.circe._
import sparxer._
import sparxer.registry.Status
import zio._
import zio.interop.catz._

case class SubmitSparkJob(
  mainClass: JobMainClass,
  master: Master,
  deployMode: DeployMode,
  jar: ApplicationJar,
  sparkConf: Map[String, String] = Map.empty,
  args: List[String] = List.empty,
  envs: Map[String, String] = Map.empty
)

case class ResubmitSparkJob(id: SparkJobId)

case class ErrorResponse(reason: String)

case class SubmittedSparkJob(id: SparkJobId)

case class GetSparkJobDetails(sparkJob: SparkJob, statuses: List[Status])

case class UserCredentials(email: EmailAddress, password: String)

case class UserToken(token: String)

object JsonInstances {

  implicit def circeEntityDecoder[A: Decoder, R]: EntityDecoder[TaskR[R, ?], A] = jsonOf[TaskR[R, ?], A]
  implicit def circeEntityEncoder[A: Encoder, R]: EntityEncoder[TaskR[R, ?], A] = jsonEncoderOf[TaskR[R, ?], A]

  implicit val emailAddressEncoderInstance: Encoder[EmailAddress] =
    Encoder.instance[EmailAddress](_.value.asJson)

  implicit val emailAddressDecoderInstance: Decoder[EmailAddress] =
    Decoder.decodeString.emap(email => EmailAddress.attempt(email).toRight(s"Invalid email format: $email"))

  implicit val sparkJobIdEncoderInstance: Encoder[SparkJobId] =
    Encoder.instance[SparkJobId](_.value.asJson)

  implicit val sparkJobIdDecoderInstance: Decoder[SparkJobId] =
    Decoder.decodeLong.emap(id => Right(SparkJobId.unsafeStatic(id)))

  implicit val jobMainClassEncoderInstance: Encoder[JobMainClass] =
    Encoder.instance[JobMainClass](_.value.asJson)

  implicit val jobMainClassDecoderInstance: Decoder[JobMainClass] =
    Decoder.decodeString.emap(mc => JobMainClass.attempt(mc).toRight(s"Invalid main class format: $mc"))

  implicit val masterEncoderInstance: Encoder[Master] =
    Encoder.instance[Master](_.uri.asJson)

  implicit val masterDecoderInstance: Decoder[Master] =
    Decoder.decodeString.emap(master => Master.attempt(master).toRight(s"Invalid master format: $master"))

  implicit val deployModeEncoderInstance: Encoder[DeployMode] =
    Encoder.instance[DeployMode](_.show.asJson)

  implicit val deployModeDecoderInstance: Decoder[DeployMode] =
    Decoder.decodeString.emap(dm => DeployMode.fromString(dm).toRight(s"Invalid deploy mode format: $dm"))

  implicit val applicationJarEncoderInstance: Encoder[ApplicationJar] =
    Encoder.instance[ApplicationJar](_.uri.asJson)

  implicit val applicationJarDecoderInstance: Decoder[ApplicationJar] =
    Decoder.decodeString.emap(jar => ApplicationJar.attempt(jar).toRight(s"Invalid application jar format: $jar"))
}
