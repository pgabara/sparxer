package sparxer

import sparxer.registry.SparkJobState
import org.apache.spark.launcher.SparkAppHandle
import org.apache.spark.launcher.SparkAppHandle.State._
import zio._
import zio.stream._

package object submit {

  // todo: simplify once https://github.com/zio/zio/issues/1040 is resolved
  def sparkJobStatuses(sah: SparkAppHandle): Stream[Nothing, SparkJobState] =
    ZStream.unwrap {
      for {
        output  <- Queue.bounded[Take[Nothing, SparkJobState]](10)
        runtime <- ZIO.runtime[Any]
        _ <- UIO {
              sah.addListener(new SparkAppHandle.Listener {
                override def infoChanged(sah: SparkAppHandle): Unit = ()
                override def stateChanged(sah: SparkAppHandle): Unit = {
                  val state = convertSparkState(sah.getState())
                  state match {
                    case _: SparkJobState.Error | _: SparkJobState.Completed =>
                      runtime.unsafeRun(output.offer(Take.Value(state)) *> output.offer(Take.End).unit)
                    case currentState =>
                      runtime.unsafeRun(output.offer(Take.Value(currentState)).unit)
                  }
                }
              })
            }
      } yield ZStream.fromQueue(output).unTake
    }

  private def convertSparkState(state: SparkAppHandle.State): SparkJobState =
    state match {
      case KILLED    => SparkJobState.Completed("The application was killed.")
      case LOST      => SparkJobState.Error("The Spark Submit JVM exited with a unknown status.")
      case UNKNOWN   => SparkJobState.Init("The application has not reported back yet.")
      case FINISHED  => SparkJobState.Completed("The application finished with a successful status.")
      case SUBMITTED => SparkJobState.InProgress("The application has been submitted to the cluster.")
      case FAILED    => SparkJobState.Error("The application finished with a failed status.")
      case RUNNING   => SparkJobState.InProgress("The application is running.")
      case CONNECTED => SparkJobState.InProgress("The application has connected to the handle.")
    }
}
