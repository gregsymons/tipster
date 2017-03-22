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

package tipster.json

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import org.joda.time.{ DateTime => DateTime }
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatter}

import spray.json._

import tipster.tips.model._

trait DateTimeSupport extends SprayJsonSupport
  with DefaultJsonProtocol 
{
  implicit object DateTimeFormat extends RootJsonFormat[DateTime] {
    val formatter = ISODateTimeFormat.basicDateTime

    def write(obj: DateTime): JsValue = {
      JsString(formatter.print(obj))
    }

    def read(json: JsValue): DateTime = {
      def attemptParse(s: String): DateTime = {
        try {
          formatter.parseDateTime(s)
        } catch {
          case t: IllegalArgumentException => fail(s)
        }
      }

      json match {
        case JsString(s) => attemptParse(s)
        case _ => fail(json.toString)
      }
    }

    def fail(value: String): DateTime = { 
      val example = formatter.print(0)
      deserializationError(s"'$value' is not a valid timestamp. Timestamps must be in compact ISO-8601 format, e.g. '$example'")
    }
  }
}

trait TipsJsonSupport extends SprayJsonSupport 
  with DefaultJsonProtocol 
  with DateTimeSupport 
{
  implicit val createTipFormat = jsonFormat2(CreateTip)
  implicit val tipFormat = jsonFormat5(Tip)
  implicit val commentFormat = jsonFormat5(Comment)
}

