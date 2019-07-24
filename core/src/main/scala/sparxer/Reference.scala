package sparxer

import zio._
import zio.clock.Clock

object Reference {
  trait Service[R] {
    val generate: ZIO[R, Nothing, Long]
  }
}

trait Reference {
  val reference: Reference.Service[Any]
}

trait ReferenceLive extends Reference with Clock {

  lazy val reference = new Reference.Service[Any] {
    val generate: ZIO[Any, Nothing, Long] = clock.nanoTime
  }
}
