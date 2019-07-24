addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val sparxer = project
  .in(file("."))
  .settings(CommonSettings)
  .aggregate(core)

lazy val core = project
  .in(file("core"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(CommonSettings)
  .settings(
    mainClass := Option("sparxer.Main"),
    scalacOptions ++= CompilerOptions,
    libraryDependencies ++= Dependencies,
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
  )

val CommonSettings = Seq(
  name := "sparxer",
  organization := "sparxer",
  version := "0.2.0",
  scalaVersion := "2.12.8",
  turbo := true
)

val CompilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-explaintypes",
  "-Yrangepos",
  "-feature",
  "-Xfuture",
  "-Ypartial-unification",
  "-language:higherKinds",
  "-language:existentials",
  "-unchecked",
  "-Yno-adapted-args",
  "-Xlint:_,-type-parameter-shadow",
  "-Xsource:2.13",
  "-Ywarn-inaccessible",
  "-Ywarn-infer-any",
  "-Ywarn-nullary-override",
  "-Ywarn-nullary-unit",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfatal-warnings"
)

val Dependencies = {
  val zioVersion        = "1.0.0-RC9-4"
  val zioCatsVersion    = "1.3.1.0-RC3"
  val doobieVersion     = "0.7.0"
  val http4sVersion     = "0.20.6"
  val circeVersion      = "0.11.1"
  val tsecVersion       = "0.1.0"
  val sparkVersion      = "2.4.3"
  val slf4jVersion      = "1.7.26"
  val logbackVersion    = "1.2.3"
  val log4catsVersion   = "0.3.0"
  val pureConfigVersion = "0.11.1"
  val specs2Version     = "4.6.0"

  Seq(
    "dev.zio"               %% "zio"                       % zioVersion,
    "dev.zio"               %% "zio-streams"               % zioVersion,
    "dev.zio"               %% "zio-interop-cats"          % zioCatsVersion,
    "dev.zio"               %% "zio-testkit"               % zioVersion % "test",
    "org.tpolecat"          %% "doobie-core"               % doobieVersion,
    "org.tpolecat"          %% "doobie-hikari"             % doobieVersion,
    "org.tpolecat"          %% "doobie-postgres"           % doobieVersion,
    "org.tpolecat"          %% "doobie-specs2"             % doobieVersion % "test",
    "org.http4s"            %% "http4s-dsl"                % http4sVersion,
    "org.http4s"            %% "http4s-circe"              % http4sVersion,
    "org.http4s"            %% "http4s-blaze-server"       % http4sVersion,
    "org.http4s"            %% "http4s-prometheus-metrics" % http4sVersion,
    "io.circe"              %% "circe-generic"             % circeVersion,
    "io.github.jmcardon"    %% "tsec-jwt-mac"              % tsecVersion,
    "io.github.jmcardon"    %% "tsec-password"             % tsecVersion,
    "org.apache.spark"      %% "spark-launcher"            % sparkVersion,
    "org.slf4j"             % "log4j-over-slf4j"           % slf4jVersion,
    "ch.qos.logback"        % "logback-classic"            % logbackVersion,
    "io.chrisdavenport"     %% "log4cats-slf4j"            % log4catsVersion,
    "com.github.pureconfig" %% "pureconfig"                % pureConfigVersion,
    "org.specs2"            %% "specs2-core"               % specs2Version % "test"
  ).map(
    _.excludeAll(
      ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j"),
      ExclusionRule(organization = "log4j")
    )
  )
}
