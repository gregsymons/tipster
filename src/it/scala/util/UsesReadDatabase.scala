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

package tipster.test.integration.util

import akka.actor._

import slick.jdbc.PostgresProfile.api._

import org.scalatest._

import tipster._

trait UsesReadDatabase extends Suite
  with BeforeAndAfterAll
{
  val system: ActorSystem
  
  val readDatabase = {
    val config = TipsterConfiguration(system)
    Database.forConfig("db", config.storageReadConfig)
  }

  override def afterAll: Unit = {
    readDatabase.close
  }
}
