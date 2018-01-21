import sbt._

object Dependencies {

  val akkaVersion         = "2.5.7"
  val akkaHttpVersion     = "10.0.11"
  val sparkVersion        = "2.2.0"
  val monixVersion        = "2.3.0"
  val configVersion       = "1.3.1"
  val catsVersion         = "1.0.1"
  val scoptVersion        = "3.7.0"
  val scalatestVersion    = "3.0.1"
  val scalacticVersion    = "3.0.1"

  lazy val akka = Seq(
    "com.typesafe.akka" %% "akka-typed"         % akkaVersion,
    "com.typesafe.akka" %% "akka-typed-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-slf4j"         % akkaVersion
  )

  lazy val akkaCluster = Seq(
    "com.typesafe.akka" %% "akka-cluster"           % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding"  % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-tools"     % akkaVersion
  )

  lazy val akkaHttp = Seq(
    "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % "test"
  )

  lazy val typesafeConfig = Seq(
    "com.typesafe" % "config" % configVersion
  )

  lazy val sparkCore = Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion
  )

  lazy val monix = Seq(
    "io.monix" %% "monix" % monixVersion
  )

  lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % catsVersion
  )

  lazy val scopt = Seq(
    "com.github.scopt" %% "scopt" % scoptVersion
  )

  lazy val tests = Seq(
    "org.scalactic" %% "scalactic" % scalacticVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion
  )

  lazy val logging = Seq(
    "ch.qos.logback"              % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging"   % "3.7.2"
  )

  object Implicits {

    implicit class DepSeqOps(deps: Seq[ModuleID]) {

      def it: Seq[ModuleID] =  deps.map(_ % "it,test")

      def test: Seq[ModuleID] = deps.map(_ % "test")
    }
  }
}
