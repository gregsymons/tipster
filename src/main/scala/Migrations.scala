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

package tipster.storage.migrations

import java.sql.{ Connection => JSqlConnection }

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor._
import akka.actor.Status._
import akka.pattern._

import org.flywaydb.core._
import org.flywaydb.core.api._
import org.flywaydb.core.api.callback._

import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState

import tipster._

class FlywayMigrator(val storageManager: ActorRef) extends Actor
  with ActorLogging
{
  import FlywayMigrator.Messages._

  val config = TipsterConfiguration(context.system)

  def flyway = {
    val flywayBean = new Flyway()
    flywayBean.configure(config.storageMigrationConfig)
    flywayBean.setCallbacks(new BaseFlywayCallback() {
      override def afterEachMigrate(ignored: JSqlConnection, info: MigrationInfo): Unit = {
        log.info(s"Completed migration: ${info}")
      }
      override def afterMigrate(ignored: JSqlConnection): Unit = {
        log.info("Migrations complete")
      }
      override def beforeEachMigrate(ignored: JSqlConnection, info: MigrationInfo): Unit = {
        log.info(s"Starting migration: ${info}")
      }
      override def beforeMigrate(ignored: JSqlConnection): Unit = {
        log.info("Starting migrations")
      }
    })
    log.info("Flyway created")
    flywayBean
  }

  override def preStart: Unit = {
    implicit val dispatcher = context.dispatcher
    log.info("FlywayMigrator starting up")

    val _ = (Future {
      log.info("Migration job started")
      val migrator = flyway
      val _ = migrator.migrate()
      MigrationComplete
    } pipeTo self)
  }

  //receive functions will always trigger this warning,
  //apparently
  @SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.Throw"))
  def receive: Receive = {
    case MigrationComplete => storageManager ! MigrationComplete
    case Failure(e) => throw categorize(e)
  }

  object Recoverable {
    lazy val RECOVERABLE_PSQL_ERRORS = Set(
      PSQLState.COMMUNICATION_ERROR.getState,
      PSQLState.CONNECTION_DOES_NOT_EXIST.getState,
      PSQLState.CONNECTION_FAILURE.getState,
      PSQLState.CONNECTION_FAILURE_DURING_TRANSACTION.getState,
      PSQLState.CONNECTION_REJECTED.getState,
      PSQLState.CONNECTION_UNABLE_TO_CONNECT.getState
    )

    def unapply(e: Throwable): Boolean = {
      e match {
        case flyway: FlywayException => {
          Option(flyway.getCause) map { cause =>
            log.info(s"Categorizing cause of FlywayException:\n$flyway\ncause: $cause")
            Recoverable.unapply(cause)
          } getOrElse false
        }
        case psql: PSQLException => {
          log.info(s"Checking PSQLException for recoverability: $psql; SQLState=${psql.getSQLState}, recoverable states=$RECOVERABLE_PSQL_ERRORS")
          if (RECOVERABLE_PSQL_ERRORS exists (_ == psql.getSQLState)) {
            log.info("Exception is recoverable")
            true
          }
          else false
        }
        case _ => false
      }
    }
  }

  def categorize(e: Throwable): Throwable = {
    e match {
      case Recoverable() => {
        log.warning(s"Recoverable exception during migration: $e")
        e
      }
      case _ => MigrationFailure(e) 
    }
  }
}

object FlywayMigrator {
  object Messages {
    case object MigrationComplete
    case class MigrationFailure(cause: Throwable) 
      extends Exception(
        s"Unrecoverable failure migrating database: ${cause.getMessage}",
        cause)
      with TipsterFatalError
  }

  def props(storageManager: ActorRef) = Props(classOf[FlywayMigrator], storageManager)
}
