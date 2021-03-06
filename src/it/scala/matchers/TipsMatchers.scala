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

package tipster.test.matchers

import org.scalatest.matchers._

import tipster.tips.model._

trait TipsMatchers {
  def username[A <: HasUsername](expected: String) = new HavePropertyMatcher[A, String] {
    def apply(message: A) =
      HavePropertyMatchResult(
        message.username == expected,
        "username",
        expected,
        message.username
      )
  }

  def message[A <: HasMessage](expected: String) = new HavePropertyMatcher[A, String] {
    def apply(message: A) =
      HavePropertyMatchResult(
        message.message == expected,
        "message",
        expected,
        message.message
      )
  }

  def id[A <: HasId](expected: Int) = new HavePropertyMatcher[A, Int] {
    def apply(message: A) =
      HavePropertyMatchResult(
        message.id == expected,
        "message",
        expected,
        message.id
      )
  }
}
