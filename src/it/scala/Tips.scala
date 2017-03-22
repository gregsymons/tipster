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

package tipster.test.integration

import scala.concurrent._
import scala.concurrent.duration._

import scala.util.Try
import scala.util.Success

import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.unmarshalling._

import akka.stream._
import akka.stream.scaladsl._

import slick.jdbc.PostgresProfile.api._
import com.github.tototoshi.slick.PostgresJodaSupport._

import org.joda.time.{DateTime => JodaDateTime}

import org.scalatest._

import tipster.test.integration.util._

import tipster._
import tipster.json._
import tipster.tips._
import tipster.tips.model._
import tipster.test.matchers.TipsMatchers

class TipsSpec extends AsyncFunSpec
  with HttpClient
  with TipsJsonSupport
  with Matchers
  with TipsMatchers
  with UsesReadDatabase
{
  implicit val executor = system.dispatcher

  def postTipRequest(tip: CreateTip): Future[(HttpRequest, CreateTip)] = {
    Marshal(tip).to[RequestEntity].map { entity =>
      val request = HttpRequest(
        uri = "/tips",
        method = HttpMethods.POST,
        entity = entity
      )
      (request, tip)
    }
  }

  def getTipRequest(tip: GetTip): (HttpRequest, GetTip) = {
    val request = HttpRequest(
      uri = s"/tips" + (tip.id.map("/" + _) getOrElse ""),
      method = HttpMethods.GET
    )

    (request, tip)
  }

  type Attempt[T] = (Try[HttpResponse], T)
  type AttemptList[T] = List[Attempt[T]]
  type ResultFilter[T] = PartialFunction[Attempt[T], Boolean]

  def onlySuccesses[T](implicit ev: Manifest[T]): ResultFilter[T] = {
     case (Success(response), _) => !response.status.isSuccess
     case _ => true
  }

  def postTip(tip: CreateTip,
             resultFilter: ResultFilter[CreateTip] = onlySuccesses): Future[AttemptList[CreateTip]] = {
    Source.fromIterator(
      () => Iterator.continually(postTipRequest(tip))
    ).throttle(1, per = 1 second, maximumBurst = 1, mode = ThrottleMode.Shaping)
     .mapAsync(1)(identity _)
     .via(tipster[CreateTip])
     .takeWhile(resultFilter, inclusive = true)
     .takeWithin(15 seconds)
     .runFold(List[(Try[HttpResponse], CreateTip)]())(_ :+ _)
  }

  def getTip(tip: GetTip): Future[List[(Try[HttpResponse], GetTip)]] = {
    Source.fromIterator(
      () => Iterator.continually(getTipRequest(tip))
    ).throttle(1, per = 1 second, maximumBurst = 1, mode = ThrottleMode.Shaping)
     .via(tipster[GetTip])
     .takeWhile({
       case (Success(response), _) => !response.status.isSuccess
       case _ => true
     }, inclusive = true)
     .takeWithin(15 seconds)
     .runFold(List[(Try[HttpResponse], GetTip)]())(_ :+ _)
  }

  def successfulAttempts[T](implicit ev: Manifest[T]):
    PartialFunction[(Try[HttpResponse], T), Boolean] =
  {
    case (Success(response), _) => response.status.isSuccess
    case _ => false
  }

  describe("The /tips resource") {
    describe("POST a new tip") {
      describe("Posting a tip with a short message") {
        it ("should work") {
          val tip = CreateTip(
            username = "juser",
            message  = "A message that will fit"
          )

          postTip(tip).map { attempts =>
            withClue(s"Attempts were ${attempts}") {
              val (maybeResponse, _) = attempts.last
              maybeResponse.fold(
                (_ => fail),
                (response => response.status.isSuccess should be (true))
              )
            }
          }
        }

        it ("should return the full tip") {
          val tip = CreateTip(
            username = "juser",
            message  = "a message that will fit"
          )

          postTip(tip).flatMap { attempts =>
            val filtered = attempts filter successfulAttempts

            filtered should not be empty

            if (filtered.nonEmpty) {
              val (Success(response), originalTip) = filtered.last

              Unmarshal(response.entity).to[Tip].map { responseTip =>
                responseTip should have (
                  (username (originalTip.username)),
                  (message  (originalTip.message))
                )
              }
            } else fail
          }
        }

        it ("should be retrievable afterwards") {
          val tip = CreateTip(
            username = "juser",
            message = "a message that will fit"
          )

          //List[(Try[HttpResponse], CreateTip)]
          postTip(tip).flatMap { putAttempts =>
            val filteredPut = putAttempts filter successfulAttempts
            filteredPut should not be empty

            if (filteredPut.nonEmpty) {
              val (Success(putResponse), _) = filteredPut.last

              Unmarshal(putResponse.entity).to[Tip].flatMap { putResponseTip =>
                getTip(GetTip(putResponseTip.id)).flatMap { getAttempts =>
                  val filteredGet = getAttempts filter successfulAttempts
                  filteredGet should not be empty

                  if (filteredGet.nonEmpty) {
                    val (Success(getResponse), _) = filteredGet.last

                    Unmarshal(getResponse.entity).to[Tip].flatMap { getResponseTip =>
                      getResponseTip should have (
                        (id (putResponseTip.id)),
                        (username (putResponseTip.username)),
                        (message (putResponseTip.message))
                      )
                    }
                  } else fail
                }
              }
            } else fail
          }
        }

        //FIXME: I would tend to prefer keeping the tests fully blackbox, but my
        //self-imposed deadlinee looms.
        it ("should be in the database") {
          val tip = CreateTip(
            username = "juser",
            message = "a message that will fit"
          )

          postTip(tip).flatMap { putAttempts =>
            val filteredPut = putAttempts filter successfulAttempts
            val (Success(response), _) = filteredPut.last

            Unmarshal(response.entity).to[Tip].flatMap { responseTip =>
              val responseId = responseTip.id
              val query = sql"""select id, username, message from tips where id = $responseId""".as[(Long, String, String)]

              readDatabase.run(query).flatMap { result =>
                result should not be empty

                if (result.nonEmpty) {
                  (result.last) should have (
                    ('_1 /* id       */ (responseTip.id)),
                    ('_2 /* username */ (responseTip.username)),
                    ('_3 /* message  */ (responseTip.message))
                  )
                } else fail
              }
            }
          }
        }
      }

      describe("Posting a tip with a message > 140 characters") {
        it ("should fail with a 400") {
          val tip = CreateTip(
            username = "juser",
            message = """
              |We only allow 140 characters for a tip, but this message is longer
              |than that (it's actually 158 characters long, which is too many
              |characters by 18 characters)
            """.stripMargin
          )

          postTip(tip, {
            case (Success(HttpResponse(status, _, _, _)), _) => status.intValue != 400 || status.intValue >= 500
            case (Success(_), _) => false
            case _ => true
          }).map { attempts =>
            val filtered = attempts.filter {
              case (Success(HttpResponse(status, _, _, _)), _) => status.intValue == 400
              case _ => false
            }

            filtered should not be empty
          }
        }
      }
    }

    describe("GET /tips") {
      it ("should return all the tips in the database") {
        //Make sure there are some tips in the database before we start,
        //but if there are more tips than these, it's fine
        val initialTips = List(
          CreateTip(username="juser", message="This is my first tip"),
          CreateTip(username="juser", message="This is my second tip")
        )


        Future.sequence(
          initialTips map { tip =>
            postTip(tip) map { attempts =>
              attempts flatMap {
                case (Success(response), tip) if response.status.isSuccess => Some(tip)
                case _ => None
              }
            }
          }
        ) flatMap { successfulPosts =>
          if (successfulPosts.flatten.nonEmpty) {
            val query = sql"select * from tips".as[(Int, String, String, JodaDateTime, JodaDateTime)]
            val tipsInDb: Future[Seq[Tip]] = readDatabase.run(query).map { resultSet =>
              resultSet.map(Tip.tupled.apply _)
            }
            val tipsFromGet = getTip(GetTip())

            tipsInDb.flatMap { dbTips =>
              tipsFromGet.flatMap { attempts =>
                withClue (s"All attempts were:\n|${attempts map { a => (a, a._1 map { r => r.status.isSuccess }) } mkString "\n|"}\n") {
                  val successfulGets = attempts filter successfulAttempts

                  withClue(s"Successful gets were:\n|${successfulGets mkString "\n|"}\n") {
                    if (successfulGets.nonEmpty) {
                      val (Success(response), _) = successfulGets.last

                      Unmarshal(response.entity).to[Seq[Tip]].map { getTips =>
                        getTips should contain allElementsOf dbTips
                      }
                    } else successfulGets should not be empty
                  }
                }
              }
            }
          } else successfulPosts.flatten should not be empty
        }
      }
    }
  }
}
