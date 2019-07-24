package sparxer.registry

import sparxer.EmailAddress
import zio._

object TestUserRegistry {
  case class Data(users: List[User])

  val EmptyData: Data = Data(List.empty[User])

  def create(users: List[User] = List.empty): UIO[TestUserRegistry] =
    Ref.make(Data(users)).map(new TestUserRegistry(_))

  def apply(data: Ref[Data]): TestUserRegistry =
    new TestUserRegistry(data)
}

final class TestUserRegistry(data: Ref[TestUserRegistry.Data]) extends UserRegistry.Service[Any] {

  def get(email: EmailAddress): IO[UserRegistryError, Option[User]] =
    data.get.map(_.users.find(_.email == email))

  def insert(user: User): ZIO[Any, UserRegistryError, Int] =
    ZIO.succeed(0)
}
