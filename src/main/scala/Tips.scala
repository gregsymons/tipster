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

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.unmarshalling._

import spray.json._

sealed trait TipMessage { }
sealed trait HasUsername {
  val username: String
}
sealed trait HasMessage {
  val message: String
}
sealed trait HasId {
  val id: Long
}

final case class GetTip(id: Long) extends TipMessage

final case class CreateTip(
  override val username: String,
  override val message: String
) extends TipMessage
  with HasUsername
  with HasMessage

final case class Tip(
  override val id: Long,
  override val username: String,
  override val message: String,
  created: String,
  updated: String
) extends TipMessage
  with HasUsername
  with HasMessage
  with HasId

trait TipsJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val createTipFormat = jsonFormat2(CreateTip)
  implicit val tipFormat = jsonFormat5(Tip)
}

trait TipsApi extends Directives
  with TipsJsonSupport
{
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def tipsRoutes: Route = 
    pathPrefix("tips") {
      pathEnd {
        post {
          entity(as[CreateTip]) { incoming =>
            completeWith(instanceOf[Tip]) { completion => 
              completion(Tip(id=1,
                             username=incoming.username,
                             message=incoming.message,
                             created="",
                             updated="")) 
            }
          }
        }
      } ~
        path(LongNumber) { id =>
          pathEnd {
            get {
                completeWith(instanceOf[Tip]) { completion => 
                  completion(Tip(id=1,
                                 username="juser",
                                 message="a message that will fit",
                                 created="",
                                 updated=""))
              }
            }
          }
        }
    }
}
