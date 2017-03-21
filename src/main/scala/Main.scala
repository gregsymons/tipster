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
import akka.pattern._
import akka.stream._
import akka.util._

import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl._

import com.typesafe

import tipster.management._
import tipster.storage._
import tipster.tips._
import tipster.tips.services._

final class TipsterGuardianStrategy extends SupervisorStrategyConfigurator {
  def create() = OneForOneStrategy() {
    case _: ConfigurationException => SupervisorStrategy.Escalate
    case _: TipsterFatalError      => SupervisorStrategy.Escalate
  }
}

trait TipsterFatalError

case class FailedToBind(
  listenAddress: String, 
  listenPort: Int,
  cause: Throwable
) extends Exception(s"Failed to bind to ${listenAddress}:${listenPort}", cause) 
  with TipsterFatalError

final class TipsterService(shutdownPromise: Promise[Done]) extends Actor
  with ActorLogging
  with ManagementApi
  with TipsApi
{
  import Http.ServerBinding
  import StorageManager.Messages._
  // These are vars because we can't initialize them until after
  // migrations are complete. Otherwise, weird things seem to happen
  // with connections not having the correct `search_path`. They're
  // never accessed outside of this actor so "unsafe" just means 
  // they're mutable.
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var unsafeTipsWriter: Option[PostgresTipsWriter] = None
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  private var unsafeTipsReader: Option[PostgresTipsReader] = None

  override def tipsWriter: Option[PostgresTipsWriter] = unsafeTipsWriter
  override def tipsReader: Option[PostgresTipsReader] = unsafeTipsReader

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()(context)
  implicit val dispatcher = context.dispatcher

  val storage = context.actorOf(StorageManager.props, "storage")
  val allRoutes = managementRoutes ~ tipsRoutes
  val config = TipsterConfiguration(context.system)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def receive: Receive = {
    case StorageReady => {
      unsafeTipsWriter = Some(PostgresTipsWriter(context.system))
      unsafeTipsReader = Some(PostgresTipsReader(context.system))
      val _ = Http().bindAndHandle(allRoutes,
                           config.apiListenAddress,
                           config.apiListenPort) pipeTo self
      context.become(binding)
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Throw"))
  def binding: Receive = {
    case serverBinding: ServerBinding => {
			system.log.info(s"Tipster Server started on http://${config.apiListenAddress}:${config.apiListenPort}")
      context.become(running(serverBinding))
    }
    case Failure(e) => throw FailedToBind(config.apiListenAddress, config.apiListenPort, e)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def running(serverBinding: ServerBinding): Receive = {
    case _ =>
  }

  override def postStop: Unit = {
    system.log.info(s"Tipster Server terminated")
    val _ = shutdownPromise.success(Done)
  }
}

object TipsterService {
  def props(shutdownPromise: Promise[Done]) = Props(classOf[TipsterService], shutdownPromise)
}

object Tipster
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
    val system  = ActorSystem("Tipster")
    val config  = TipsterConfiguration(system)
		val shutdown =  {
			val shutdownPromise = Promise[Done]
      val service = system.actorOf(TipsterService.props(shutdownPromise), "tipster")
			shutdownPromise.future
		}

		val _ = Await.ready(shutdown, Duration.Inf)
  }
}
