package sparxer.submit

import sparxer.SparkJob
import sparxer.registry.SparkJobState
import zio._
import zio.stream._

final class TestSparkJobLauncher(data: Ref[TestSparkJobLauncher.Data]) extends SparkJobLauncher.Service[Any] {

  def start(job: SparkJob): ZIO[Any, SparkLauncherError, Stream[Nothing, SparkJobState]] =
    data.get.flatMap { state =>
      state.returnError match {
        case Some(error) => ZIO.fail(error)
        case None        => data.modify(d => (Stream.fromIterable(d.states), d.copy(jobs = job :: d.jobs)))
      }
    }
}

object TestSparkJobLauncher {

  case class Data(
    jobs: List[SparkJob] = List.empty,
    states: List[SparkJobState] = List.empty,
    returnError: Option[SparkLauncherError] = None
  )

  val EmptyData: Data = Data(List.empty[SparkJob], states = List.empty[SparkJobState], Option.empty[SparkLauncherError])

  def apply(data: Ref[Data]): TestSparkJobLauncher =
    new TestSparkJobLauncher(data)
}
