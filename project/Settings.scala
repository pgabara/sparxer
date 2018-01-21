import sbt._
import sbt.Keys._
import scoverage.ScoverageKeys._

import Dependencies._

object Settings {

  val ProjectName = "sparxer"

  val commonSettings = Seq(
    name          := ProjectName,
    organization  := "com.github.bhop",
    version       := "0.0.1",
    scalaVersion  := "2.11.12",


    coverageMinimum       := 70,
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
    ),

    libraryDependencies ++= cats ++ akka ++ typesafeConfig ++ logging
  )
}
