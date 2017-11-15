package com.github.bhop.sparxer.adapter

import java.nio.file.Path

import monix.reactive.Observable

object domain {

  case class Spark(home: Path, master: String, mode: String = "client", props: Map[String, String] = Map.empty)

  case class SparkApp(name: String, jar: String, main: String, args: List[String] = List.empty)
  case class JobConfig(app: SparkApp, verbose: Boolean = true)

  case class JobState(jobId: Option[String], state: String)
  case class JobSubscription(subscriptionId: String, states: Observable[JobState])
}
