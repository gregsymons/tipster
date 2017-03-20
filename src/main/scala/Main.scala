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

import java.lang.Thread

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import akka._
import akka.actor._
import akka.stream._

import akka.http.scaladsl.model._
import akka.http.scaladsl._

import com.typesafe

import tipster.management._
import tipster.storage._
import tipster.tips._

final class TipsterGuardianStrategy extends SupervisorStrategyConfigurator {
  def create() = OneForOneStrategy() {
    case _: ConfigurationException => SupervisorStrategy.Escalate
    case _: TipsterFatalError      => SupervisorStrategy.Escalate
  }
}

trait TipsterFatalError

object Tipster extends ManagementApi with TipsApi
{
  val banner = """
  |  _______ _____  _____  _______ _______ _______  ______
  |     |      |   |_____] |______    |    |______ |_____/
  |     |    __|__ |       ______|    |    |______ |    \_
  |                                                       
  |  Starting Up...
  """.stripMargin

  def main(args: Array[String]): Unit = {
		println(banner)

    implicit val system = ActorSystem("Tipster")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    val config = TipsterConfiguration(system)

    val storage = system.actorOf(Props[StorageManager], "storage")

    val allRoutes = managementRoutes ~ tipsRoutes

    val binding = Http().bindAndHandle(allRoutes, config.apiListenAddress, config.apiListenPort)
		binding.onComplete { _ =>
			system.log.info(s"Tipster Server started on http://${config.apiListenAddress}:${config.apiListenPort}")
		}

		val shutdown =  {
			val shutdownPromise = Promise[Done]
			val _ = sys.addShutdownHook {
				((binding.transformWith {
					case Success(serverBinding) => serverBinding.unbind.flatMap(_ => Future.successful(Done))
					case Failure(_) => Future.successful(Done)
				}).transformWith { _ =>
					system.log.info(s"Tipster Server terminated")
					system.terminate
				}).onComplete(_ => shutdownPromise.success(Done))
			}
			shutdownPromise.future
		}

		val _ = Await.ready(shutdown, Duration.Inf)
  }
}
