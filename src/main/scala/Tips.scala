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

package tipster.tips

import scala.concurrent._

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.unmarshalling._

import org.joda.time.{ DateTime => JodaDateTime }

import tipster.json._

object model {
  sealed trait TipMessage { }
  sealed trait HasUsername {
    val username: String
  }
  sealed trait HasMessage {
    val message: String
  }
  sealed trait HasId {
    val id: Int
  }

  final case class GetTip(override val id: Int) 
    extends TipMessage
    with HasId

  final case class CreateTip(
    override val username: String,
    override val message: String
  ) extends TipMessage
    with HasUsername
    with HasMessage

  final case class Tip(
    override val id: Int,
    override val username: String,
    override val message: String,
    created: JodaDateTime,
    updated: JodaDateTime
  ) extends TipMessage
    with HasUsername
    with HasMessage
    with HasId

  //Explicitly extend Function5 so Slick auto mapping works.
  object Tip extends Function5[Int,
                               String,
                               String,
                               JodaDateTime,
                               JodaDateTime,
                               Tip] {
    val MAX_MESSAGE_LEN = 140
  }
}

object services {
  import model._

  trait TipsWriter {
    def createTip(tip: CreateTip): Future[Tip]
  }

  trait TipsReader {
    def findTip(tip: GetTip): Future[Option[Tip]]
  }
}

trait TipsApi extends Directives
  with TipsJsonSupport
{
  import model._
  import services._

  def tipsWriter: Option[TipsWriter]
  def tipsReader: Option[TipsReader]

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def tipsRoutes: Route = 
    pathPrefix("tips") {
      pathEnd {
        post {
          tipsWriter map { writer =>
            entity(as[CreateTip]) { incoming =>
              validate(incoming.message.length <= Tip.MAX_MESSAGE_LEN,
                       "Message too long")
              {
                onComplete(writer.createTip(incoming)) { tip =>
                  complete(tip)
                }
              }
            }
          } getOrElse complete(StatusCodes.InternalServerError)
        }
      } ~
        path(IntNumber) { id =>
          pathEnd {
            get {
              tipsReader map { reader =>
                onSuccess(reader.findTip(GetTip(id))) {
                  case Some(tip) => complete(tip)
                  case None => complete(StatusCodes.NotFound)
                }
              } getOrElse complete(StatusCodes.InternalServerError)
            }
          }
        }
    }
}
