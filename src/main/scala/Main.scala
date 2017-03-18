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

package tipster

import scala.concurrent._

import akka._
import akka.actor._

import akka.http.scaladsl.model._
import akka.http.scaladsl.server._

import tipster.management._


final class Tipster extends HttpApp 
  with ManagementApi
{
  override def route: Route = managementRoutes

  override def waitForShutdownSignal(actorSystem: ActorSystem)(implicit ec: ExecutionContext): Future[Done] = {
    val promise = Promise[Done]()
    val _ = sys.addShutdownHook {
      val _ = promise.success(Done)
    }
    promise.future
  }
}

object Tipster {
  def main(args: Array[String]): Unit = {

    val app = new Tipster
    //TODO: configurate interface and port
    app.startServer("0.0.0.0", 8080)
  }
}
