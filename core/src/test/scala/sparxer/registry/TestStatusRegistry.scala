package sparxer.registry

import sparxer.SparkJobId
import zio._

import java.time.Instant

final class TestStatusRegistry(data: Ref[TestStatusRegistry.Data]) extends StatusRegistry.Service[Any] {

  def get(jobId: SparkJobId): ZIO[Any, StatusRegistryError, List[Status]] =
    data.get.map(_.statuses.filter(_.jobId == jobId))

  def insert(jobId: SparkJobId, state: SparkJobState): ZIO[Any, StatusRegistryError, Status] =
    data.modify { d =>
      val status = Status(jobId, state, d.staticInstant)
      (status, d.copy(statuses = status :: d.statuses, staticInstant = d.staticInstant.plusSeconds(1)))
    }
}

object TestStatusRegistry {

  case class Data(statuses: List[Status], staticInstant: Instant = Instant.ofEpochMilli(0))

  val EmptyData: Data = Data(List.empty[Status], Instant.ofEpochMilli(0))

  def apply(data: Ref[Data]): TestStatusRegistry =
    new TestStatusRegistry(data)
}
