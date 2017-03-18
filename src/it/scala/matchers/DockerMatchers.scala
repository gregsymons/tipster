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

import com.spotify.docker.client.messages._

object DockerMatchers {
  trait ContainerMatchers {
    
    def image(expected: String) = new HavePropertyMatcher[Container, String] {
      def apply(container: Container) = 
        HavePropertyMatchResult(
          container.image == expected,
          "image",
          expected,
          container.image
        )
    }

    def state(expected: String) = new HavePropertyMatcher[Container, String] {
      def apply(container: Container) =
        HavePropertyMatchResult(
          container.state.toLowerCase == expected.toLowerCase,
          "state",
          expected.toLowerCase,
          container.status.toLowerCase
        )
    }
  }
}
