import Settings._
import Dependencies._
import Dependencies.Implicits._

lazy val sparxer = project.in(file("."))
  .settings(commonSettings)
  .settings(libraryDependencies ++= scopt ++ tests.test)
  .dependsOn(http, engine)
  .aggregate(protocol, engine, http)
  .enablePlugins(JavaAppPackaging)

lazy val protocol = project.in(file("protocol"))
  .settings(commonSettings)
  .settings(name := ProjectName + "-protocol")
  .settings(libraryDependencies ++= akka ++ monix ++ tests.test)
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    )
  )

lazy val engine = project.in(file("engine"))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(commonSettings)
  .settings(name := ProjectName + "-engine")
  .settings(libraryDependencies ++= akkaCluster ++ sparkCore ++ monix ++ tests.it)
  .dependsOn(protocol)

lazy val http = project.in(file("http"))
  .settings(commonSettings)
  .settings(name := ProjectName + "-http")
  .settings(libraryDependencies ++= akkaHttp ++ akkaCluster ++ monix ++ tests.test)
  .dependsOn(protocol)