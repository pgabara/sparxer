package com.github.bhop.sparxer.http.adapters

import java.util.concurrent.ThreadLocalRandom

import akka.Done
import akka.actor.Scheduler
import akka.util.Timeout
import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.AskPattern._
import akka.typed.receptionist.Receptionist.{Listing, Subscribe}
import akka.typed.scaladsl.Actor
import monix.eval.Task
import com.github.bhop.sparxer.adapter.domain.JobConfig
import com.github.bhop.sparxer.engine.SparkEngine
import com.typesafe.scalalogging.StrictLogging

class ClusterSparkEngine(engine: ActorRef[ClusterSparkEngine.Command])
                        (implicit timeout: Timeout, scheduler: Scheduler) extends SparkEngineAdapter {

  import ClusterSparkEngine._

  override def submit(config: JobConfig): Task[Done] =
    Task.fromFuture[Event](engine ? (Submit(config, _))).flatMap {
      case Submitted => Task.now(Done)
      case Error(message) => Task.raiseError(new IllegalStateException(message))
    }
}

object ClusterSparkEngine extends StrictLogging {

  sealed trait Command
  case class Submit(config: JobConfig, replayTo: ActorRef[Event]) extends Command
  private case class UpdateInstances(listing: Listing[SparkEngine.Command]) extends Command

  sealed trait Event
  case object Submitted extends Event
  case class Error(message: String) extends Event

  def behavior: Behavior[Command] =
    Actor.deferred { context =>
      val receptionistAdapter = context.spawnAdapter(UpdateInstances)
      context.system.receptionist ! Subscribe(SparkEngine.ReceptionistKey, receptionistAdapter)
      active()
    }

  private[adapters] def active(instances: Vector[ActorRef[SparkEngine.Command]] = Vector.empty): Behavior[Command] =
    Actor.immutable { (_, message) =>
      message match {
        case UpdateInstances(Listing(_, updatedInstances)) =>
          logger.info(s"Number of active submitters in the cluster changed: ${instances.size} => ${updatedInstances.size}")
          active(updatedInstances.toVector)

        case Submit(config, replayTo) =>
          if (instances.isEmpty) replayTo ! Error("There are no registered spark engines in the cluster")
          else {
            val i = ThreadLocalRandom.current().nextInt(instances.size)
            instances(i) ! SparkEngine.Submit(config)
            replayTo ! Submitted
          }
          Actor.same
      }
    }
}
