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

val akkaVersion = "2.4.17"
val akkaHttpVersion = "10.0.4"
val scalaTestVersion = "3.0.1"

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
      BuildInfoOption.ToMap,
      BuildInfoOption.BuildTime
    ),
    dockerBaseImage := "openjdk:8-jre-alpine",
    dockerExposedPorts := Seq(8080),
    (defaultLinuxInstallLocation in Docker) := s"/srv/${name.value}",
    (integrate in IntegrationTest) := Def.sequential(
      (publishLocal in Docker),
      dockerCompose.toTask(" up --force-recreate -d"),
      (test in IntegrationTest),
      dockerCompose.toTask(" down")
    ).value,
    // App Dependencies
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor"  % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j"  % akkaVersion,
      "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion),
    // Test Dependencies
    libraryDependencies ++= Seq(
      "org.scalactic" %% "scalactic" % scalaTestVersion % "it,test",
      "org.scalatest" %% "scalatest" % scalaTestVersion % "it,test"
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
    wartremoverErrors ++= Warts.unsafe
  )
