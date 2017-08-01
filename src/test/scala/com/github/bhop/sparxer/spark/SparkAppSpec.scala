package com.github.bhop.sparxer.spark

import akka.actor.ActorSystem
import akka.testkit._
import akka.util.Timeout
import com.github.bhop.sparxer.AkkaSpec

import scala.concurrent.duration._
import monix.execution.Scheduler.Implicits.global

class SparkAppSpec extends TestKit(ActorSystem("test")) with ImplicitSender with AkkaSpec with TestSparkApi {

  import domain._
  import SparkApp._

  implicit val timeout = Timeout(5.seconds)

  "A Spark App" when {

    "an active app" should {

      "unsubscribe app (stop actor & return last reported state)" in {
        val app = system.actorOf(props("test-spark-app", testSubmission()))
        val watcher = TestProbe()
        watcher watch app

        app ! Unsubscribe
        expectMsg(Unsubscribed(State("STARTED", isFinal = false)))

        watcher.expectTerminated(app)
      }

      "unsubscribe when final state reported" in {
        val app = system.actorOf(props("test-spark-app", testSubmission(statesChain = Seq(
          State("RUNNING", isFinal = false), State("FINISHED", isFinal = true)
        ))))

        val watcher = TestProbe()
        watcher watch app

        watcher.expectTerminated(app)
      }

      "continue if reported stated is not final one" in {
        val app = system.actorOf(props("test-spark-app", testSubmission(statesChain = Seq(
          State("CONNECTED", isFinal = false), State("RUNNING", isFinal = false)
        ))))

        askUntil[CurrentState](app, GetState) { state =>
          state == CurrentState(State("RUNNING", isFinal = false))
        }
      }

      "get current state" in {
        val app = system.actorOf(props("test-spark-app", testSubmission()))
        app ! GetState
        expectMsg(CurrentState(State("STARTED", isFinal = false)))
      }

      "kill app and stop actor" in {
        val app = system.actorOf(props("test-spark-app", testSubmission()))

        val watcher = TestProbe()
        watcher watch app

        app ! Kill
        expectMsg(Killed(State("DISCONNECTED", isFinal = false)))

        watcher.expectTerminated(app)
      }
    }
  }

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)
}
