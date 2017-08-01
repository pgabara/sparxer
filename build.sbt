import Settings._
import Dependencies._

lazy val sparxer = project.in(file("."))
  .settings(commonSettings)
  .settings(
    libraryDependencies
      ++= catsDependencies
      ++ monixDependencies
      ++ akkaActorDependencies
      ++ akkaStreamDependencies
      ++ akkaHttpDependencies
      ++ sparkDependencies
      ++ loggingDependencies
      ++ scalaTestDependencies
  )