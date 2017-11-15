import Settings._
import Dependencies._

lazy val sparxer = project.in(file("."))
  .settings(commonSettings)
  .settings(libraryDependencies ++= akka ++ akkaCluster ++ akkaHttp ++ monix)
  .dependsOn(adapter)
  .aggregate(adapter)

lazy val adapter = project.in(file("adapter"))
  .settings(commonSettings)
  .settings(name := "sparxer-adapter")
  .settings(libraryDependencies ++= sparkCore ++ monix)