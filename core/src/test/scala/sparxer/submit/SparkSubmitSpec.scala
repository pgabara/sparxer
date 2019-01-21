package sparxer.submit

import sparxer._
import org.specs2.Specification
import sparxer.registry.{ SparkJobState, Status, StatusRegistry, TestStatusRegistry }
import zio._

import java.time.Instant

final class SparkSubmitSpec extends Specification with DefaultRuntime {

  def is = "Spark Submit".title ^ s2"""
    Submit new spark job:
      launch job via SparkJobLauncher                     $launchSparkJob
      register spark job with init status                 $registerSparkJob
      track updates and register them                     $trackUpdates
      report error and update registry with error status  $reportError
    """

  private def launchSparkJob =
    unsafeRun(
      for {
        launcherData     <- Ref.make(TestSparkJobLauncher.EmptyData)
        registryData     <- Ref.make(TestStatusRegistry.EmptyData)
        env              = environment(launcherData, registryData)
        _                <- env.sparkSubmit.submit(sparkJob)
        testLauncherData <- launcherData.get
      } yield testLauncherData.jobs
    ) must be_===(List(sparkJob))

  private def registerSparkJob =
    unsafeRun(
      for {
        launcherData     <- Ref.make(TestSparkJobLauncher.EmptyData)
        registryData     <- Ref.make(TestStatusRegistry.EmptyData)
        env              = environment(launcherData, registryData)
        _                <- env.sparkSubmit.submit(sparkJob)
        testRegistryData <- registryData.get
      } yield testRegistryData.statuses
    ) must be_===(List(Status(sparkJob.id, SparkJobState.Init("Application registered."), Instant.ofEpochMilli(0))))

  private def trackUpdates =
    unsafeRun(
      for {
        launcherData     <- Ref.make(TestSparkJobLauncher.Data(List.empty, statuses))
        registryData     <- Ref.make(TestStatusRegistry.EmptyData)
        env              = environment(launcherData, registryData)
        _                <- env.sparkSubmit.submit(sparkJob)
        testRegistryData <- registryData.get.repeat(ZSchedule.doUntil(_.statuses.size == 4))
      } yield testRegistryData.statuses.map(_.state)
    ) must be_===(statuses.reverse ++ List(SparkJobState.Init("Application registered.")))

  private def reportError =
    unsafeRun(
      for {
        launcherData     <- Ref.make(TestSparkJobLauncher.Data(returnError = Option(SparkLauncherError("test error!"))))
        registerData     <- Ref.make(TestStatusRegistry.EmptyData)
        env              = environment(launcherData, registerData)
        _                <- env.sparkSubmit.submit(sparkJob).either.unit
        testRegistryData <- registerData.get
      } yield testRegistryData.statuses.map(_.state)
    ) must be_===(List(SparkJobState.Error("test error!"), SparkJobState.Init("Application registered.")))

  type TestEnv = SparkSubmitLive with StatusRegistry with SparkJobLauncher

  private def environment(
    launcherData: Ref[TestSparkJobLauncher.Data],
    registryData: Ref[TestStatusRegistry.Data]
  ): SparkSubmitLive =
    new SparkSubmitLive {
      val sparkJobLauncher: SparkJobLauncher.Service[Any] = TestSparkJobLauncher(launcherData)
      val statusRegistry: StatusRegistry.Service[Any]     = TestStatusRegistry(registryData)
    }

  private val sparkJob = SparkJob(
    SparkJobId.unsafeStatic(10),
    JobMainClass.unsafe("Main.scala"),
    Master.unsafe("local[*]"),
    DeployMode.Cluster,
    ApplicationJar.unsafe("test.jar"),
    EmailAddress.unsafe("test@test.com"),
    Map("a" -> "b", "c" -> "d")
  )

  private val statuses =
    List(SparkJobState.Init("init"), SparkJobState.InProgress("in progress"), SparkJobState.Completed("completed"))
}
