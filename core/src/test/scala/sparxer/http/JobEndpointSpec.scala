package sparxer.http

import sparxer._
import sparxer.registry._
import sparxer.http.RouteTesting._
import io.circe.generic.auto._
import org.http4s.{ Request, Uri }
import org.http4s.Status._
import org.specs2.Specification
import zio._

import java.time.Instant

final class JobEndpointSpec extends Specification with DefaultRuntime {

  import JsonInstances._

  def is = "Job Endpoint".title ^ s2"""
    Get spark job with a given id:
      return 200 and spark job details                        $return200
      return 404 if spark job with a given id is not defined  $return404
      return 401 if user is not authorized                    $return401
    Get user jobs:
      return empty list if user does not have submitted jobs  $emptyUserJobs
      return a list of user submitted jobs                    $userJobs
    """

  private def return200 =
    unsafeRun(
      for {
        registryState       <- Ref.make(TestJobRegistry.Data(List(sparkJob)))
        statusRegistryState <- Ref.make(TestStatusRegistry.Data(statuses))
        env                 = environment(registryState, statusRegistryState)
        routes              = new JobEndpoint[JobEnv].routes
        response            <- testRoute[JobEnv, GetSparkJobDetails](routes, getSparkJobRequest).provide(env)
      } yield response
    ) must be_===(Ok -> GetSparkJobDetails(sparkJob, statuses))

  private def return404 =
    unsafeRun(
      for {
        registryState       <- Ref.make(TestJobRegistry.EmptyData)
        statusRegistryState <- Ref.make(TestStatusRegistry.Data(statuses))
        env                 = environment(registryState, statusRegistryState)
        routes              = new JobEndpoint[JobEnv].routes
        response            <- testRoute[JobEnv, ErrorResponse](routes, getSparkJobRequest).provide(env)
      } yield response
    ) must be_===(NotFound -> ErrorResponse("Spark job with id: 10 not found."))

  private def return401 =
    unsafeRun(
      for {
        registryState       <- Ref.make(TestJobRegistry.EmptyData)
        statusRegistryState <- Ref.make(TestStatusRegistry.Data(statuses))
        env                 = environment(registryState, statusRegistryState)
        routes              = new JobEndpoint[JobEnv].routes
        response            <- testRouteResponse[JobEnv](routes, getSparkJobRequest, unauthorizedAuthMiddleware).provide(env)
      } yield response.status
    ) must be_===(Unauthorized)

  private def emptyUserJobs =
    unsafeRun(
      for {
        registryState       <- Ref.make(TestJobRegistry.EmptyData)
        statusRegistryState <- Ref.make(TestStatusRegistry.EmptyData)
        env                 = environment(registryState, statusRegistryState)
        routes              = (new JobEndpoint[JobEnv]).routes
        response            <- testRoute[JobEnv, List[SparkJob]](routes, getUserJobsRequest).provide(env)
      } yield response
    ) must be_===(Ok -> List.empty[SparkJob])

  private def userJobs =
    unsafeRun(
      for {
        registryState       <- Ref.make(TestJobRegistry.Data(List(sparkJob)))
        statusRegistryState <- Ref.make(TestStatusRegistry.EmptyData)
        env                 = environment(registryState, statusRegistryState)
        routes              = new JobEndpoint[JobEnv].routes
        response            <- testRoute[JobEnv, List[SparkJob]](routes, getUserJobsRequest).provide(env)
      } yield response
    ) must be_===(Ok -> List(sparkJob))

  private def environment(
    jobRegistryState: Ref[TestJobRegistry.Data],
    statusRegistryState: Ref[TestStatusRegistry.Data]
  ): JobEnv =
    new JobRegistry with StatusRegistry {
      val jobRegistry: JobRegistry.Service[Any]       = TestJobRegistry(jobRegistryState)
      val statusRegistry: StatusRegistry.Service[Any] = TestStatusRegistry(statusRegistryState)
    }

  private val sparkJob: SparkJob =
    SparkJob(
      SparkJobId.unsafeStatic(10),
      JobMainClass.unsafe("Main.scala"),
      Master.unsafe("local[*]"),
      DeployMode.Cluster,
      ApplicationJar.unsafe("test.jar"),
      EmailAddress.unsafe("test@test.com")
    )

  private val statuses =
    List(
      Status(SparkJobId.unsafeStatic(10), SparkJobState.Init("init"), Instant.ofEpochMilli(0)),
      Status(SparkJobId.unsafeStatic(10), SparkJobState.Error("test error!"), Instant.ofEpochMilli(1))
    )

  private def getSparkJobRequest[F[_]]: Request[F] =
    Request[F](uri = Uri.unsafeFromString("/10"))

  private def getUserJobsRequest[F[_]]: Request[F] =
    Request[F](uri = Uri.unsafeFromString("/"))
}
