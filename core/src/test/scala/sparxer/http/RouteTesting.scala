package sparxer.http

import cats.data.Kleisli
import cats.data.OptionT
import org.http4s.{ AuthedRoutes, EntityDecoder, Request, Response, Status }
import org.http4s.syntax.kleisli._
import org.http4s.server.AuthMiddleware
import sparxer.EmailAddress
import sparxer.http.Auth.UserData
import zio._
import zio.interop.catz._
import org.http4s.HttpRoutes

object RouteTesting {

  def testRoute[R, A: EntityDecoder[TaskR[R, ?], ?]](
    routes: AuthedRoutes[UserData, TaskR[R, ?]],
    request: Request[TaskR[R, ?]],
    middleware: AuthMiddleware[TaskR[R, ?], UserData] = authorizedAuthMiddleware[R]
  ): TaskR[R, (Status, A)] =
    for {
      response <- testRouteResponse(routes, request, middleware)
      json     <- response.as[A]
    } yield (response.status, json)

  def testRouteResponse[R](
    routes: AuthedRoutes[UserData, TaskR[R, ?]],
    request: Request[TaskR[R, ?]],
    middleware: AuthMiddleware[TaskR[R, ?], UserData] = authorizedAuthMiddleware[R]
  ): TaskR[R, Response[TaskR[R, ?]]] =
    middleware(routes).orNotFound.run(request)

  def testRouteResponseR[R](
    routes: HttpRoutes[TaskR[R, ?]],
    request: Request[TaskR[R, ?]]
  ): TaskR[R, Response[TaskR[R, ?]]] =
    routes.orNotFound.run(request)

  def authorizedAuthMiddleware[R]: AuthMiddleware[TaskR[R, ?], UserData] =
    AuthMiddleware(Kleisli(_ => OptionT.liftF(TaskR.succeed(UserData(EmailAddress.unsafe("test@test.com"))))))

  def unauthorizedAuthMiddleware[R]: AuthMiddleware[TaskR[R, ?], UserData] =
    AuthMiddleware(Kleisli(_ => OptionT.none))
}
