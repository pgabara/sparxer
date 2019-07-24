package sparxer.submit

import sparxer.SparkJob
import zio._

final class TestSparkSubmit(data: Ref[TestSparkSubmit.Data]) extends SparkSubmit.Service[Any] {

  def submit(job: SparkJob): ZIO[Any, SparkSubmitError, Unit] =
    data.get.flatMap { state =>
      state.returnError match {
        case Some(e) => ZIO.fail(e) *> ZIO.unit
        case None    => data.modify(d => ((), d.copy(jobs = job :: d.jobs)))
      }
    }
}

object TestSparkSubmit {

  case class Data(jobs: List[SparkJob], returnError: Option[SparkSubmitError])

  val EmptyData: Data = Data(List.empty[SparkJob], Option.empty[SparkSubmitError])

  def apply(data: Ref[Data]): TestSparkSubmit =
    new TestSparkSubmit(data)
}
