package sparxer.registry

import sparxer.{ SparkJob, SparkJobId }
import zio._
import sparxer.EmailAddress

object TestJobRegistry {
  case class Data(jobs: List[SparkJob])

  val EmptyData: Data = Data(List.empty[SparkJob])

  def create(jobs: List[SparkJob] = List.empty): UIO[TestJobRegistry] =
    Ref.make(Data(jobs)).map(new TestJobRegistry(_))

  def apply(data: Ref[Data]): TestJobRegistry =
    new TestJobRegistry(data)
}

final class TestJobRegistry(data: Ref[TestJobRegistry.Data]) extends JobRegistry.Service[Any] {

  def register(job: SparkJob): IO[JobRegistryError, SparkJob] =
    data.modify(d => (job, TestJobRegistry.Data(job :: d.jobs)))

  def get(id: SparkJobId): IO[JobRegistryError, Option[SparkJob]] =
    data.get.map(_.jobs.find(_.id == id))

  def getUserJobs(user: EmailAddress): IO[JobRegistryError, List[SparkJob]] =
    data.get.map(_.jobs.filter(_.owner == user))

  val get: UIO[List[SparkJob]] =
    data.get.map(_.jobs)
}
