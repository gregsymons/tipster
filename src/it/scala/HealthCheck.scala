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

import tipster.test.integration.util._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Try
import scala.util.Success
import scala.util.Failure

import akka.NotUsed
import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.stream._
import akka.stream.scaladsl._
import Source._
import Sink._
import Http.HostConnectionPool

import org.scalatest._
import org.scalatest.Matchers._

class HealthCheckSpec extends AsyncFunSpec
  with HttpClient
{
  type HealthCheckRequest = Tuple2[HttpRequest, NotUsed]
  type HealthCheckAttempt = Try[HttpResponse]
  type HealthCheckAttemptRaw = Tuple2[HealthCheckAttempt, NotUsed]
  type HealthCheckFlow = Flow[HealthCheckRequest, HealthCheckAttemptRaw, HostConnectionPool]
  val healthCheck: HealthCheckFlow = tipster[NotUsed]

  def attemptHealthCheck: Source[HealthCheckAttempt, NotUsed] = Source.fromIterator(
    () => Iterator.continually(
      (HttpRequest(uri="http://172.24.0.50/health"), NotUsed)
  )).throttle(1, per=1 second, maximumBurst = 1, mode = ThrottleMode.Shaping)
    .via(healthCheck)
    .takeWhile({
      case (Success(response), _) => !response.status.isSuccess
      case _ => true
    }, inclusive = true)
    .takeWithin(1 minute)
    .map {
      case (Success(response), _) => Success(response)
      case (failure, _)           => failure
    }

  describe("The /health resource") {
    it("should be accessible at http://172.24.0.50/health") {
        val eventuallyAttempts = attemptHealthCheck.runFold(List[HealthCheckAttempt]())(_ :+ _)
        eventuallyAttempts map { attempts: List[HealthCheckAttempt] =>
          atLeast (1, attempts) shouldBe a [Success[HttpResponse]] 
          atLeast (1, attempts) should matchPattern {
              case Success(response) if response.asInstanceOf[HttpResponse].status.isSuccess =>
          }
        }
    }
  }
}
