package sparxer.http

import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import zio._
import zio.interop.catz._

final class HealthCheckEndpoint[R] extends Http4sDsl[TaskR[R, ?]] {

  type HealthIO[A] = TaskR[R, A]

  val routes: HttpRoutes[HealthIO] =
    HttpRoutes.of {
      case _ @GET -> Root / "live"  => Ok("I am healthy!")
      case _ @GET -> Root / "ready" => Ok("I am ready!")
    }
}
