package com.github.bhop.sparxer.spark

import akka.actor.{Actor, Props}
import com.typesafe.scalalogging.LazyLogging

import monix.eval.Task
import monix.execution.Scheduler

import domain._

object SparkApp {

  case object Unsubscribe
  case class Unsubscribed(state: State)

  case object Kill
  case class Killed(state: State)

  case object GetState
  case class CurrentState(state: State)

  private case class StateChanged(state: State)

  def props(name: String, submission: Submission)(implicit S: Scheduler): Props =
    Props(new SparkApp(name, submission))
}

class SparkApp(name: String, submission: Submission)(implicit S: Scheduler) extends Actor with LazyLogging {

  import SparkApp._

  private val handler = submission.handler.onChange(state => self ! StateChanged(state))

  def receive: Receive = active(submission.id, handler.state, handler.disconnect)

  def active(id: String, state: State, disconnect: () => Task[State]): Receive = {

    case GetState =>
      sender() ! CurrentState(state)

    case Kill =>
      logger.info(s"Killing spark app: $name. Current state: $state [id: $id]")
      val replyTo = sender()
      disconnect().foreach { state =>
        replyTo ! Killed(state)
        context.stop(self)
      }

    case Unsubscribe =>
      logger.info(s"Unsubscribing app: $name. Current state: $state [id: $id]")
      if (sender() != self) sender() ! Unsubscribed(state)
      context.stop(self)

    case StateChanged(newState) =>
      logger.info(s"State changed for app: $name. New state: $newState [id: $id]")
      if (newState.isFinal) self ! Unsubscribe
      else context.become(active(id, newState, disconnect))
  }
}
