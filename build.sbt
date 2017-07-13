import Settings._
import Dependencies._

lazy val sparxer = project.in(file("."))
  .settings(commonSettings)
  .settings(libraryDependencies ++= scalaTestDependencies)
  .dependsOn(submitter)

lazy val submitter = project.in(file("submitter"))
  .settings(commonSettings)
  .settings(name := "sparxer-submitter")