package com.github.bhop.sparxer.adapter

import monix.execution.Scheduler
import org.apache.spark.launcher.SparkAppHandle
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

class SparkAppHandleToObservableTest extends WordSpec with Matchers with ScalaFutures {

  implicit val io: Scheduler = Scheduler.io()

  "A SparkAppHandle to Observable converter" should {

    "convert SparkAppHandle instance into monix Observable" in {
      val handler = new StubSparkAppHandle()
      val converter = new implicits.SparkAppHandleToObservable(handler)
      val states = converter.toObservable.map(_.state).foldLeftL(List.empty[String]) { (acc, state) => state :: acc }
      states.runAsync.futureValue should be(List("FINISHED", "RUNNING", "SUBMITTED", "CONNECTED"))
    }
  }
}

class StubSparkAppHandle(state: SparkAppHandle.State = SparkAppHandle.State.UNKNOWN) extends SparkAppHandle {

  override def disconnect(): Unit = ()

  override def kill(): Unit = ()

  override def getAppId = "stub-app-id"

  override def getState: SparkAppHandle.State = state

  override def stop(): Unit = ()

  override def addListener(l: SparkAppHandle.Listener): Unit = {
    import SparkAppHandle.State._
    val states = List(CONNECTED, SUBMITTED, RUNNING, FINISHED)
    states.foreach(s => l.stateChanged(new StubSparkAppHandle(s)))
  }
}
