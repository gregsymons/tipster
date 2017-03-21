/**
 * 
 * © Copyright 2017 Greg Symons <gsymons@gsconsulting.biz>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

name := "tipster"

organization := "biz.gsconsulting.tipster"

version := "0.1.0"

val akkaVersion           = "2.4.17"
val akkaHttpVersion       = "10.0.4"
val dockerClientVersion   = "8.1.2"
val flywayVersion         = "4.1.2"
val logbackVersion        = "1.1.3"
val scalaTestVersion      = "3.0.1"
val slickVersion          = "3.2.0"
val postgresDriverVersion = "42.0.0"

val integrate = taskKey[Unit]("Run integration tests against the dockerized environment")

val tipster = (project in file(".")).
  enablePlugins(JavaServerAppPackaging,
                AshScriptPlugin,
                DockerPlugin,
                BuildInfoPlugin).
  configs(IntegrationTest).
  settings(Defaults.itSettings: _*).
  settings(
    buildInfoPackage := "tipster",
    buildInfoOptions ++= Seq(
      Some(BuildInfoOption.ToMap),
      // Only put the build time in for CI builds so that
      // local builds can leverage docker caching.
      sys.env.get("CI") map { _ => BuildInfoOption.BuildTime }
    ).flatten,
    dockerBaseImage := "openjdk:8-jre-alpine",
    dockerExposedPorts := Seq(8080),
    (defaultLinuxInstallLocation in Docker) := s"/srv/${name.value}",
    dockerComposeEnvVars := Seq(
      ("TIPSTER_DB_MIGRATIONS_CLEAN_ON_VALIDATE_FAILURE", "true")
    ),
    // I have no idea how to suppress the "discarded non-Unit value here.
    (integrate in IntegrationTest) := Def.sequential(
      (test in Test),
      (compile in IntegrationTest),
      (publishLocal in Docker),
      dockerCompose.toTask(" up --force-recreate -d"),
      (test in IntegrationTest),
      dockerCompose.toTask(" down -v --remove-orphans")
    ).value,
    // App Dependencies
    libraryDependencies ++= Seq(
      "ch.qos.logback"      % "logback-classic" % logbackVersion,
      "com.typesafe.akka"  %% "akka-actor"      % akkaVersion,
      "com.typesafe.akka"  %% "akka-stream"     % akkaVersion,
      "com.typesafe.akka"  %% "akka-slf4j"      % akkaVersion,
      "com.typesafe.akka"  %% "akka-http"       % akkaHttpVersion,
      "com.typesafe.akka"  %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.slick" %% "slick"           % slickVersion,
      "com.typesafe.slick" %% "slick-hikaricp"  % slickVersion,
      "org.flywaydb"        % "flyway-core"     % flywayVersion,
      "org.postgresql"      % "postgresql"      % postgresDriverVersion
    ),
    // Test Dependencies
    libraryDependencies ++= Seq(
      "org.scalactic"     %% "scalactic"         % scalaTestVersion    % "it,test",
      "org.scalatest"     %% "scalatest"         % scalaTestVersion    % "it,test",
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion     % "test",
      "com.spotify"        % "docker-client"     % dockerClientVersion % "it"
    ),
    scalacOptions ++= Seq(
      "-target:jvm-1.8",
      "-encoding", "UTF-8",
      "-unchecked",
      "-deprecation",
      "-Xfuture",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Ywarn-unused",
      "-feature",
      "-language:implicitConversions",
      "-language:postfixOps"
    ),
    scalaVersion := "2.12.1",
    (wartremoverErrors in (Compile, compile)) ++= Warts.unsafe
  )
