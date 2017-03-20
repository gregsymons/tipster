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

package tipster.test.integration.util

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.stream._

import org.scalatest._

trait HttpClient extends Suite with BeforeAndAfterAll {
  implicit val system = ActorSystem(getClass.getSimpleName)
  implicit val materializer = ActorMaterializer()
  lazy val http = Http()

  def tipster[A] = http.cachedHostConnectionPool[A](host="172.24.0.50")

  override def afterAll: Unit = {
    val _ = Await.result(system.terminate, 1 minute)
  }
}
