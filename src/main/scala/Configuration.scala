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

package tipster

import akka.actor._
import com.typesafe.config.Config

final class TipsterConfigurationExtension(config: Config) extends Extension {
  val apiListenAddress = config.getString("tipster.api.address")
  val apiListenPort    = config.getInt("tipster.api.port")
}

object TipsterConfiguration extends ExtensionId[TipsterConfigurationExtension] 
  with ExtensionIdProvider
{
  override def lookup = TipsterConfiguration

  override def createExtension(system: ExtendedActorSystem) =
    new TipsterConfigurationExtension(system.settings.config)
}
