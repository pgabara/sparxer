package com.github.bhop.sparxer.spark

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import monix.execution.Scheduler

object SparkSubmitter {

  case class Submit(app: domain.App)
  case class Submitted(submissionId: String)

  def apply(spark: domain.Spark, submit: domain.SparkAppSubmission)(implicit S: Scheduler): SparkSubmitter =
    new SparkSubmitter(spark, submit)
}

class SparkSubmitter(spark: domain.Spark, submit: domain.SparkAppSubmission)(implicit S: Scheduler) extends Actor
  with LazyLogging {

  import SparkSubmitter._

  def receive: Receive = {

    case Submit(app) =>
      logger.info(s"Submitting new app: ${app.name} with args: [ ${app.args.mkString(", ")} ]")
      val replyTo = sender()
      submit(spark, app)
        .flatMap(submission => spawn(app.name, submission))
        .foreach(id => replyTo ! Submitted(id))
  }

  private def spawn(name: String, submission: domain.Submission) =
    Task.eval(context.actorOf(SparkApp.props(name, submission), submission.id))
      .map(_ => submission.id)
}
