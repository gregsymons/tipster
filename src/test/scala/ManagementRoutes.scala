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

package tipster.test

import akka.http.scaladsl.testkit._

import org.scalatest._

import tipster.management._

class ManagementRoutesSpec extends FunSpec 
  with Matchers
  with ScalatestRouteTest 
{
  object Api extends ManagementApi {
    val routes = managementRoutes
  }

  import Api._

  describe("The Tipster Management API") {
    describe("The /health route") {
      it("returns the buildinfo") {
        Get("/health") ~> routes ~> check {
          responseAs[String] should include("Build Info")
        }
      }
    }
  }
}
