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

package tipster.storage

import scala.concurrent._
import scala.concurrent.duration._

import akka._
import akka.pattern._
import akka.actor._

import slick.jdbc.PostgresProfile.api._
import com.github.tototoshi.slick.PostgresJodaSupport._

import com.typesafe.config.Config

import org.joda.time._

import tipster.TipsterConfiguration
import tipster.storage.migrations._
import tipster.tips.services._
import tipster.tips.model._

//TODO: Generate these from a migrated database rather than by hand
object TipsTables {
  class Tips(tag: Tag) extends Table[Tip](tag, "tips") {
    def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username")
    def message  = column[String]("message")
    def created  = column[DateTime]("created")
    def updated  = column[DateTime]("updated")
    def * = (id, username, message, created, updated) <> (Tip.tupled, Tip.unapply)
  }

  val tips = TableQuery[Tips]
}

trait PostgresQueryExecutor {

  val system: ActorSystem
  def config: Config

  val database = Database.forConfig("db", config)
  implicit val executor = system.dispatcher
}

final case class PostgresTipsWriter(override val system: ActorSystem) 
  extends TipsWriter 
  with PostgresQueryExecutor
{
  import TipsTables._

  override def config = TipsterConfiguration(system).storageWriteConfig

  override def createTip(tip: CreateTip): Future[Tip] = {
    database.run(
      (tips.map(c => (c.username, c.message))
        returning tips.map(c => (c.id, c.username, c.message, c.created, c.updated))
        into ((_, fullTip) => Tip.tupled.apply(fullTip))) += ((tip.username, tip.message))
    ) 
  }
}

final case class PostgresTipsReader(override val system: ActorSystem) 
  extends TipsReader 
  with PostgresQueryExecutor
{
  import TipsTables._
  
  override def config = TipsterConfiguration(system).storageReadConfig

  override def findTip(tip: GetTip): Future[Option[Tip]] = {
    database.run(tips.filter(_.id === tip.id).result.headOption)
  }
}

class StorageManager extends Actor
  with ActorLogging
{
  import FlywayMigrator.Messages._
  import StorageManager.Messages._

  override val supervisorStrategy = OneForOneStrategy() {
    case _: ConfigurationException => SupervisorStrategy.Escalate
    case _: MigrationFailure => SupervisorStrategy.Escalate
  }

  override def preStart: Unit = {
    log.info("Tipster storage subsystem starting up")
    self ! StartMigration
  }

  //receive functions will always trigger this warning,
  //apparently
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override def receive: Receive = {
    case StartMigration => {
      val supervisorProps = BackoffSupervisor.props(
        Backoff.onFailure(
          childProps   = FlywayMigrator.props(self),
          childName    = "migrator",
          minBackoff   = 3 seconds,
          maxBackoff   = 30 seconds,
          randomFactor = 0.2
        ).withAutoReset(10 seconds)
         .withSupervisorStrategy(
           OneForOneStrategy() {
             case _: ConfigurationException => SupervisorStrategy.Escalate
             case _: MigrationFailure => SupervisorStrategy.Escalate
           }
        )
      )

      val supervisor = context.actorOf(supervisorProps, "migration-supervisor")
      context.become(migrating(supervisor))
    }
  }

  //receive functions will always trigger this warning,
  //apparently
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def migrating(migrationSupervisor: ActorRef): Receive = {
    case MigrationComplete => {
      context.stop(migrationSupervisor)
      context.parent ! StorageReady
      context.become(ready)
    }
  }

  //receive functions will always trigger this warning,
  //apparently
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def ready: Receive = {
    case _ =>
  }
}

object StorageManager {
  object Messages {
    case object StartMigration
    case object StorageReady
  }

  def props = Props(classOf[StorageManager])
}

