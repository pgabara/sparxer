import sbt._

object Dependencies {

  val catsVersion = "0.9.0"
  val monixVersion = "2.3.0"
  val sparkVersion = "2.2.0"
  val akkaVersion = "2.5.3"
  val akkaHttpVersion = "10.0.9"
  val logbackVersion = "1.2.3"
  val scalaLoggingVersion = "3.7.1"
  val scalaTestVersion = "3.0.1"
  val scalacticVersion = "3.0.1"

  lazy val catsDependencies = Seq(
    "org.typelevel" %% "cats" % catsVersion
  )

  lazy val monixDependencies = Seq(
    "io.monix" %% "monix" % monixVersion
  )

  lazy val sparkDependencies = Seq(
    "org.apache.spark" %% "spark-launcher" % sparkVersion
  )

  lazy val akkaActorDependencies = Seq(
    "com.typesafe.akka" %% "akka-actor"   % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
  )

  lazy val akkaStreamDependencies = Seq(
    "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion % "test"
  )

  lazy val akkaHttpDependencies = Seq(
    "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % "test"
  )

  lazy val loggingDependencies = Seq(
    "ch.qos.logback"              % "logback-classic" % logbackVersion,
    "com.typesafe.scala-logging" %% "scala-logging"   % scalaLoggingVersion
  )

  lazy val scalaTestDependencies = Seq(
    "org.scalactic" %% "scalactic" % scalacticVersion % "test",
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  )
}
