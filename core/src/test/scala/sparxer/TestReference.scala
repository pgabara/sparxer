package sparxer

import zio._

object TestReference {
  case class Data(static: Long)

  def create(static: Long): UIO[TestReference] =
    Ref.make(Data(static)).map(new TestReference(_))
}

final class TestReference private (data: Ref[TestReference.Data]) extends Reference.Service[Any] {
  val generate: zio.UIO[Long] = data.get.map(_.static)
}
