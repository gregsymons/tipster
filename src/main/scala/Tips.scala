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

import akka._
import akka.stream._
import akka.stream.scaladsl._

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.common._
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
  
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  final case class GetTip(id: Option[Int] = None) extends TipMessage 

  object GetTip extends TipMessage{
    def apply(id: Int): GetTip = GetTip(Some(id))
  }

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

  sealed trait CommentMessage { }

  final case class CreateComment(
    tipId: Int,
    username: String,
    comment: String
  ) extends CommentMessage

  final case class GetComment(
    tipId: Int,
    id: Option[Int]
  )

  final case class Comment(
    id: Int,
    tipId: Int,
    username: String,
    comment: String,
    created: JodaDateTime
  ) extends CommentMessage
}

object services {
  import model._

  trait TipsWriter {
    def createTip(tip: CreateTip): Future[Tip]
    def createComment(comment: CreateComment): Future[Comment]
  }

  trait TipsReader {
    def findTip(tip: GetTip): Future[Option[Tip]]
    def getAllTips: Source[Tip, NotUsed]
    def getAllComments(comments: GetComment): Source[Comment, NotUsed]
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
  def tipsRoutes: Route = {
    implicit val jsess = EntityStreamingSupport.json

    pathPrefix("tips") {
      pathEnd {
        post {
          tipsWriter map { writer =>
            entity(as[CreateTip]) { incoming =>
              validate(incoming.message.length <= Tip.MAX_MESSAGE_LEN,
                       "Message too long")
              {
                complete(writer.createTip(incoming)) 
              }
            }
          } getOrElse complete(StatusCodes.InternalServerError)
        } ~
        get {
          tipsReader map { reader =>
            complete(reader.getAllTips)
          } getOrElse complete(StatusCodes.InternalServerError)
        }
      } ~
        pathPrefix(IntNumber) { tipId =>
          pathEnd {
            get {
              tipsReader map { reader =>
                onSuccess(reader.findTip(GetTip(tipId))) {
                  case Some(tip) => complete(tip)
                  case None => complete(StatusCodes.NotFound)
                }
              } getOrElse complete(StatusCodes.InternalServerError)
            }
          } ~
            pathPrefix("comments") { 
              pathEnd {
                post {
                  tipsWriter map { writer =>
                    entity(as[Map[String, String]]) { incoming =>
                      (validate(incoming.get("username").nonEmpty, "username field is required") &
                       validate(incoming.get("comment").nonEmpty, "comment field is required")) {
                         complete(
                           writer.createComment(
                             CreateComment(
                               tipId = tipId,
                               username = incoming("username"),
                               comment = incoming("comment"))))
                       }
                    }
                  } getOrElse complete(StatusCodes.InternalServerError)
                } ~
                get {
                  tipsReader map { reader =>
                    complete(reader.getAllComments(GetComment(tipId = tipId, id=None)))
                  } getOrElse complete(StatusCodes.InternalServerError)
                }
              }
            }
        } 
    }
  }
}
