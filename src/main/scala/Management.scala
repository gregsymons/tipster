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

package tipster.management

import akka.http.scaladsl.model._
import akka.http.scaladsl.server._

import tipster.BuildInfo

trait ManagementApi extends Directives {
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def managementRoutes: Route =
    path("health") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"""
          |<h1>Tipster v${BuildInfo.version}</h1>
          |
          |<h2>Build Info:</h2>
          |<table>
          | ${(BuildInfo.toMap
                       .toList
                       .sortBy(_._1)
                       .map { case (k, v) =>
                          s"<tr><td>${k}</td><td>${v}</td></tr>"
                       }).mkString("\n  ")}
          |</table>
        """.stripMargin))
      }
    }
}
