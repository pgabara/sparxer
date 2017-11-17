package com.github.bhop.sparxer.adapter

import org.apache.spark.launcher.SparkAppHandle
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import monix.execution.Scheduler

import com.github.bhop.sparxer.adapter.domain.JobState

class SparkAppHandleToObservableTest extends WordSpec with Matchers with ScalaFutures {

  implicit val io: Scheduler = Scheduler.io()

  "A SparkAppHandle to monix Observable converter" should {

    "convert a SparkAppHandle instance into a monix Observable instance" in {
      val converter = new implicits.SparkAppHandleToObservable(new StubSparkAppHandle())
      val states = converter.toObservable.map(_.state).foldLeftL(List.empty[String]) { (acc, state) => state :: acc }
      states.runAsync.futureValue should be(List("FINISHED", "RUNNING", "SUBMITTED", "CONNECTED"))
    }

    "handle SparkAppHandle internal exception" in {
      val converter = new implicits.SparkAppHandleToObservable(new StubSparkAppHandle(fail = true))
      val states = converter.toObservable.onErrorHandle(_ => JobState(None, "Error!"))
      states.runAsyncGetFirst.futureValue should be(Some(JobState(None, "Error!")))
    }
  }
}

class StubSparkAppHandle(state: SparkAppHandle.State = SparkAppHandle.State.UNKNOWN,
                         fail: Boolean = false) extends SparkAppHandle {

  override def disconnect(): Unit = ()

  override def kill(): Unit = ()

  override def getAppId = "stub-app-id"

  override def getState: SparkAppHandle.State = state

  override def stop(): Unit = ()

  override def addListener(l: SparkAppHandle.Listener): Unit = {
    import SparkAppHandle.State._
    if (fail) throw new IllegalStateException("Something is wrong!")
    val states = List(CONNECTED, SUBMITTED, RUNNING, FINISHED)
    states.foreach(s => l.stateChanged(new StubSparkAppHandle(s)))
  }
}
