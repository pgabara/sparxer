package com.github.bhop.sparxer.protocol.engine

import com.github.bhop.sparxer.protocol.spark.Spark.JobState

object JobTracker {

  sealed trait JobTrackerCommand
  case class UpdateJobState(state: JobState) extends JobTrackerCommand
  case object TerminateJobTracker extends JobTrackerCommand

  sealed trait JobTrackerEvent
  case class JobStateUpdated(id: String, state: JobState) extends JobTrackerEvent
}
