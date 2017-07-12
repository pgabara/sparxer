import sbt._

object Dependencies {

  val scalaTestVersion = "3.0.1"
  val scalacticVersion = "3.0.1"

  lazy val scalaTestDependencies = Seq(
    "org.scalactic" %% "scalactic" % scalacticVersion % "test",
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  )
}
