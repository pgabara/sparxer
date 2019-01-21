package sparxer.http

import io.circe.generic.auto._
import org.specs2.Specification
import org.http4s.{ Method, Request, Status, Uri }
import sparxer._
import sparxer.http.RouteTesting._
import sparxer.submit.{ SparkSubmit, SparkSubmitError, TestSparkSubmit }
import sparxer.registry.{ JobRegistry, TestJobRegistry }
import zio._

final class SubmitEndpointSpec extends Specification with DefaultRuntime {

  import JsonInstances._

  def is = "Submit Endpoint".title ^ s2"""
    Submit new spark job:
      parse json and register given spark job               $registerSparkJob
      return submitted spark job id                         $returnSubmittedSparkJobId
      return spark submit error                             $returnSparkSubmitError
    Resubmit spark job:
      return 404 if job with a given id is not defined      $resubmitNotFound
      return spark submit error                             $resubmitSparkSubmitError
      register job with new id                              $resubmitRegisterNewJob
      return submitted spark job id                         $resubmitReturnSparkJobId
    """

  private def registerSparkJob =
    unsafeRun(
      for {
        registryState     <- Ref.make(TestJobRegistry.EmptyData)
        submitState       <- Ref.make(TestSparkSubmit.EmptyData)
        env               <- environment(registryState, submitState)
        routes            = (new SubmitEndpoint[SubmitEnv]).routes
        _                 <- testRoute[SubmitEnv, SubmittedSparkJob](routes, submitSparkJobRequest).provide(env)
        testRegistryState <- registryState.get
        testSubmitState   <- submitState.get
      } yield (testRegistryState.jobs, testSubmitState.jobs)
    ) must be_===(List(sparkJob) -> List(sparkJob))

  private def returnSubmittedSparkJobId =
    unsafeRun(
      for {
        registryState <- Ref.make(TestJobRegistry.EmptyData)
        submitState   <- Ref.make(TestSparkSubmit.EmptyData)
        env           <- environment(registryState, submitState)
        routes        = (new SubmitEndpoint[SubmitEnv]).routes
        response      <- testRoute[SubmitEnv, SubmittedSparkJob](routes, submitSparkJobRequest).provide(env)
      } yield response
    ) must be_===(Status.Created -> SubmittedSparkJob(sparkJob.id))

  private def returnSparkSubmitError =
    unsafeRun(
      for {
        registryState <- Ref.make(TestJobRegistry.EmptyData)
        submitState   <- Ref.make(TestSparkSubmit.Data(List.empty, Option(SparkSubmitError("test error!"))))
        env           <- environment(registryState, submitState)
        routes        = (new SubmitEndpoint[SubmitEnv]).routes
        response      <- testRoute[SubmitEnv, ErrorResponse](routes, submitSparkJobRequest).provide(env)
      } yield response
    ) must be_===(Status.InternalServerError -> ErrorResponse("test error!"))

  private def resubmitNotFound =
    unsafeRun(
      for {
        registryState <- Ref.make(TestJobRegistry.EmptyData)
        submitState   <- Ref.make(TestSparkSubmit.EmptyData)
        env           <- environment(registryState, submitState)
        routes        = (new SubmitEndpoint[SubmitEnv]).routes
        response      <- testRoute[SubmitEnv, ErrorResponse](routes, resubmitSparkJobRequest).provide(env)
      } yield response
    ) must be_===(Status.NotFound -> ErrorResponse("Job with id: 10 does not exist."))

  private def resubmitSparkSubmitError =
    unsafeRun(
      for {
        registryState <- Ref.make(TestJobRegistry.Data(List(sparkJob)))
        submitState   <- Ref.make(TestSparkSubmit.Data(List.empty, Option(SparkSubmitError("test error!"))))
        env           <- environment(registryState, submitState)
        routes        = (new SubmitEndpoint[SubmitEnv]).routes
        response      <- testRoute[SubmitEnv, ErrorResponse](routes, resubmitSparkJobRequest).provide(env)
      } yield response
    ) must be_===(Status.InternalServerError -> ErrorResponse("test error!"))

  private def resubmitRegisterNewJob = {
    val resubmittedSparkJob = sparkJob.copy(id = SparkJobId.unsafeStatic(15))
    unsafeRun(
      for {
        registryState     <- Ref.make(TestJobRegistry.Data(List(sparkJob)))
        submitState       <- Ref.make(TestSparkSubmit.EmptyData)
        env               <- environment(registryState, submitState, staticReference = 15)
        routes            = (new SubmitEndpoint[SubmitEnv]).routes
        _                 <- testRoute[SubmitEnv, SubmittedSparkJob](routes, resubmitSparkJobRequest).provide(env)
        testRegistryState <- registryState.get
        testSubmitState   <- submitState.get
      } yield (testRegistryState.jobs, testSubmitState.jobs)
    ) must be_===((List(resubmittedSparkJob, sparkJob), List(resubmittedSparkJob)))
  }

  private def resubmitReturnSparkJobId =
    unsafeRun(
      for {
        registryState <- Ref.make(TestJobRegistry.Data(List(sparkJob)))
        submitState   <- Ref.make(TestSparkSubmit.EmptyData)
        env           <- environment(registryState, submitState, staticReference = 15)
        routes        = (new SubmitEndpoint[SubmitEnv]).routes
        response      <- testRoute[SubmitEnv, SubmittedSparkJob](routes, resubmitSparkJobRequest).provide(env)
      } yield response
    ) must be_===(Status.Created -> SubmittedSparkJob(SparkJobId.unsafeStatic(15)))

  private val sparkJob = SparkJob(
    SparkJobId.unsafeStatic(10),
    JobMainClass.unsafe("Main.scala"),
    Master.unsafe("local[*]"),
    DeployMode.Cluster,
    ApplicationJar.unsafe("test.jar"),
    EmailAddress.unsafe("test@test.com"),
    Map.empty,
    List("a", "b")
  )

  private def environment(
    registryState: Ref[TestJobRegistry.Data],
    submitState: Ref[TestSparkSubmit.Data],
    staticReference: Long = 10
  ): UIO[SubmitEnv] =
    TestReference.create(staticReference).map { testReference =>
      new JobRegistry with SparkSubmit with Reference {
        val jobRegistry: JobRegistry.Service[Any] = TestJobRegistry(registryState)
        val sparkSubmit: SparkSubmit.Service[Any] = TestSparkSubmit(submitState)
        val reference: Reference.Service[Any]     = testReference
      }
    }

  private val submitSparkJobRequest: Request[TaskR[SubmitEnv, ?]] =
    Request[TaskR[SubmitEnv, ?]](method = Method.POST, uri = Uri.unsafeFromString("/submit"))
      .withEntity(
        SubmitSparkJob(
          JobMainClass.unsafe("Main.scala"),
          Master.unsafe("local[*]"),
          DeployMode.Cluster,
          ApplicationJar.unsafe("test.jar"),
          Map.empty,
          List("a", "b")
        )
      )

  private val resubmitSparkJobRequest: Request[TaskR[SubmitEnv, ?]] =
    Request[TaskR[SubmitEnv, ?]](method = Method.POST, uri = Uri.unsafeFromString("/resubmit"))
      .withEntity(ResubmitSparkJob(SparkJobId.unsafeStatic(10)))
}
