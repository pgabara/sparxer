package com.github.bhop.sparxer.protocol.spark

import monix.reactive.Observable

object Spark {

  case class SparkInstance(home: String, master: String, mode: String = "client", props: Map[String, String] = Map.empty)

  case class SparkApp(name: String, jar: String, main: String, args: List[String] = List.empty)
  case class JobConfig(app: SparkApp, verbose: Boolean = true)

  case class JobState(jobId: Option[String], state: String)
  case class JobSubscription(id: String, states: Observable[JobState])
}
