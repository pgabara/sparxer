package com.github.bhop.sparxer.spark
import monix.eval.Task

trait TestSparkApi {

  import domain._

  def testSubmission(id: String = "submission-id", statesChain: Seq[State] = Seq.empty) =
    Submission(id, DummyStateHandler(chain = statesChain))

  case class DummyStateHandler(init: State = State("STARTED", isFinal = false),
                               chain: Seq[State] = Seq.empty) extends StateHandler {

    def state: State = init

    def disconnect(): Task[State] = Task.now(State("DISCONNECTED", isFinal = false))

    def onChange(f: (State) => Unit): StateHandler = {
      chain.foreach(f)
      DummyStateHandler(init)
    }
  }
}
