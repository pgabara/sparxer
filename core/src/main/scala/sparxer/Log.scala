package sparxer

import io.chrisdavenport.log4cats.StructuredLogger
import zio._

object Log {

  def debug(message: => String)(implicit L: StructuredLogger[Task]): UIO[Unit] =
    L.debug(message).ignore

  def info(message: => String)(implicit L: StructuredLogger[Task]): UIO[Unit] =
    L.info(message).ignore

  def warn(message: => String)(implicit L: StructuredLogger[Task]): UIO[Unit] =
    L.warn(message).ignore

  def error(message: => String)(implicit L: StructuredLogger[Task]): UIO[Unit] =
    L.error(message).ignore

  def error(t: Throwable, message: => String)(implicit L: StructuredLogger[Task]): UIO[Unit] =
    L.error(t)(message).ignore
}
