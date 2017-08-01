package com.github.bhop.sparxer.spark

import monix.eval.Task

object domain {

  type SparkAppSubmission = (Spark, App) => Task[Submission]

  case class Spark(home: String, master: String = "local[*]", mode: String = "client", props: Map[String, String] = Map.empty)
  case class App(name: String, jar: String, main: String, args: Seq[String] = Seq.empty)
  case class Submission(id: String, handler: StateHandler)
  case class State(name: String, isFinal: Boolean)

  trait StateHandler {
    def state: State
    def disconnect(): Task[State]
    def onChange(f: State => Unit): StateHandler
  }
}
