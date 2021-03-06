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

package tipster.build

import sbt._
import sbt.Keys._
import sbt.complete.DefaultParsers._

object DockerComposePlugin extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    val dockerCompose = inputKey[String]("Run a raw docker-compose command and return its output")
    val dockerComposeCommand = settingKey[String]("The path to the docker-compose command")
    val dockerComposeEnvVars = settingKey[Seq[(String, String)]]("Environment variables to set when running docker-compose")
  }

  import autoImport._

  override def projectSettings = Seq(
    dockerCompose := {
      val args = spaceDelimited("<command> <options>").parsed
      execDockerCompose(
        dockerComposeCommand.value,
        args,
        dockerComposeEnvVars.value,
        streams.value.log)
    },
    dockerComposeCommand := "docker-compose",
    dockerComposeEnvVars := Seq()
  )

  def execDockerCompose(command: String,
                        args: Seq[String], 
                        environment: Seq[(String, String)],
                        log: Logger
  ): String = {
    val envString = environment map { e =>
      val (envvar, value) = e
      s"$envvar=$value"
    } mkString " "
    val cmdline = s"env ${envString} $command ${args mkString " "}"
    log.info(s"Executing command: $cmdline")
    cmdline !!
  }
}
