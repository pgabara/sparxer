package com.github.bhop.sparxer.protocol.engine

import java.io.NotSerializableException

import akka.actor.{ActorSystem, ExtendedActorSystem}
import akka.typed.testkit.Inbox
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import com.github.bhop.sparxer.protocol.engine.SparkEngine.{JobSubmitted, SparkEngineEvent, SubmitJob}
import com.github.bhop.sparxer.protocol.spark.Spark.{JobConfig, SparkApp}

class EngineSerializerSpec extends WordSpec with Matchers with BeforeAndAfterAll {

  private val system = ActorSystem("test")
  private val serializer = new EngineSerializer(system.asInstanceOf[ExtendedActorSystem])

  "An Engine Serializer" should {

    "decode manifest" in {
      serializer.manifest(stubSubmitJob) should be("aa")
      serializer.manifest(JobSubmitted(id = "1")) should be("ab")
    }

    "throw error if manifest not recognized" in {
      intercept[IllegalArgumentException] {
        serializer.manifest("undefined")
      }
    }

    "serialize and deserialize SubmitJob message" in {
      val serialized = serializer.toBinary(stubSubmitJob)
      val deserialized = serializer.fromBinary(serialized, "aa").asInstanceOf[SubmitJob]
      deserialized.config should be(stubSubmitJob.config) // fixme: actorRefs have different paths
    }

    "serialize and deserialize JobSubmitted message" in {
      val serialized = serializer.toBinary(JobSubmitted(id = "39"))
      serializer.fromBinary(serialized, "ab") should be(JobSubmitted(id = "39"))
    }

    "throw error if an object not recognized and cannot be serialized" in {
      intercept[IllegalArgumentException] {
        serializer.toBinary("undefined")
      }
    }

    "throw error if manifest not recognized and bytes cannot be deserialized" in {
      intercept[NotSerializableException] {
        serializer.fromBinary("unhandled".getBytes, "unhandled")
      }
    }
  }

  private def stubSubmitJob: SubmitJob =
    SubmitJob(JobConfig(SparkApp("test", "test.jar", "Test")), Inbox[SparkEngineEvent]("test").ref)

  override def afterAll(): Unit = system.terminate()
}
