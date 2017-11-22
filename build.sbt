import Settings._
import Dependencies._

lazy val sparxer = project.in(file("."))
  .settings(commonSettings)
  .aggregate(protocol, engine, http)

lazy val protocol = project.in(file("protocol"))
  .settings(commonSettings)
  .settings(name := ProjectName + "-protocol")
  .settings(libraryDependencies ++= akka ++ monix)

lazy val engine = project.in(file("engine"))
  .settings(commonSettings)
  .settings(name := ProjectName + "-engine")
  .settings(libraryDependencies ++= akka ++ akkaCluster ++ sparkCore ++ monix)
  .dependsOn(protocol)

lazy val http = project.in(file("http"))
  .settings(commonSettings)
  .settings(name := ProjectName + "-http")
  .settings(libraryDependencies ++= akka ++ akkaHttp ++ akkaCluster ++ monix)
  .dependsOn(protocol)