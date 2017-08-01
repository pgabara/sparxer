import sbt._
import sbt.Keys._
import scoverage.ScoverageKeys._

object Settings {

  val commonSettings = Seq(
    name          := "sparxer",
    organization  := "com.github.bhop",
    version       := "0.0.1",
    scalaVersion  := "2.11.11",


    coverageMinimum       := 80,
    coverageFailOnMinimum := true,

    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-Ywarn-dead-code",
      "-Ywarn-infer-any",
      "-Ywarn-unused-import",
      "-Xfatal-warnings",
      "-Xlint"
    ),

    resolvers ++= Seq(
      Resolver sonatypeRepo "public",
      Resolver typesafeRepo "releases"
    )
  )
}
