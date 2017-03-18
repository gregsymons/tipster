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

import scala.concurrent.duration._

import akka.actor._
import akka.pattern._

class StorageManager extends Actor
  with ActorLogging
{
  import FlywayMigrator.Messages._
  import StorageManager.Messages._

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
  }
}

class FlywayMigrator(val storageManager: ActorRef) extends Actor
  with ActorLogging
{
  import FlywayMigrator.Messages._

  override def preStart: Unit = {
    log.info("FlywayMigrator starting up")
    storageManager ! MigrationComplete
  }

  //receive functions will always trigger this warning,
  //apparently
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def receive: Receive = {
    case _ =>
  }
}

object FlywayMigrator {
  object Messages {
    case object MigrationComplete
  }

  def props(storageManager: ActorRef) = Props(classOf[FlywayMigrator], storageManager)
}
