package com.github.bhop.sparxer.protocol.engine

import java.io.NotSerializableException

import akka.typed.cluster.ActorRefResolver
import akka.typed.scaladsl.adapter._
import akka.serialization.{BaseSerializer, SerializerWithStringManifest}
import com.gihutb.bhop.sparxer.protocol.Messages.{JobConfigProto, JobSubmittedProto, SparkAppProto, SubmitJobProto}
import com.github.bhop.sparxer.protocol.engine.SparkEngine.{JobSubmitted, SubmitJob}
import com.github.bhop.sparxer.protocol.spark.Spark.{JobConfig, SparkApp}

class EngineSerializer(val system: akka.actor.ExtendedActorSystem) extends SerializerWithStringManifest
  with BaseSerializer {

  private val resolver = ActorRefResolver(system.toTyped)

  private val SubmitJobManifest    = "aa"
  private val JobSubmittedManifest = "ab"

  override def manifest(o: AnyRef): String = o match {
    case _: SubmitJob => SubmitJobManifest
    case _: JobSubmitted => JobSubmittedManifest
    case _ =>
      throw new IllegalArgumentException(s"Can't serialize object of type ${o.getClass} in [${getClass.getName}]")
  }

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case a: SubmitJob => submitJobToBinary(a)
    case a: JobSubmitted => jobSubmittedToBinary(a)
    case _ =>
      throw new IllegalArgumentException(s"Cannot serialize object of type [${o.getClass.getName}]")
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case SubmitJobManifest => binaryToSubmitJob(bytes)
    case JobSubmittedManifest => binaryToJobSubmitted(bytes)
    case _ =>
      throw new NotSerializableException(
        s"Unimplemented deserialization of message with manifest [$manifest] in [${getClass.getName}]")
  }

  private def submitJobToBinary(a: SubmitJob): Array[Byte] =
    SubmitJobProto(
      config = Some(JobConfigProto(
        app = Some(SparkAppProto(a.config.app.name, a.config.app.jar, a.config.app.main, a.config.app.args)),
        verbose = a.config.verbose
      )),
      replyTo = resolver.toSerializationFormat(a.replyTo)
    ).toByteArray

  private def jobSubmittedToBinary(a: JobSubmitted): Array[Byte] =
    JobSubmittedProto(a.id).toByteArray

  private def binaryToSubmitJob(bytes: Array[Byte]): SubmitJob = {
    val message = for {
      proto  <- Option(SubmitJobProto.parseFrom(bytes))
      config <- proto.config
      parsed <- parseJobConfig(config)
    } yield SubmitJob(parsed, replyTo = resolver.resolveActorRef(proto.replyTo))
    message match {
      case Some(msg) => msg
      case None => throw new IllegalArgumentException("Cannot deserialize SubmitJob message")
    }
  }

  private def parseJobConfig(c: JobConfigProto): Option[JobConfig] = {
    c.app.map { app =>
      JobConfig(
        app = SparkApp(name = app.name, jar = app.jar, main = app.main, args = app.args.toList),
        verbose = c.verbose
      )
    }
  }

  private def binaryToJobSubmitted(bytes:Array[Byte]): JobSubmitted = {
    val a = JobSubmittedProto.parseFrom(bytes)
    JobSubmitted(id = a.id)
  }
}
