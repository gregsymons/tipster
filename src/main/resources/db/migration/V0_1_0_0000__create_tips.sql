------
-- 
-- © Copyright 2017 Greg Symons <gsymons@gsconsulting.biz>.
--
-- Licensed under the Apache License, Version 2.0 (the "License"); you may not
-- use this file except in compliance with the License. You may obtain a copy of
-- the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
-- WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
-- License for the specific language governing permissions and limitations under
-- the License.
--
------

CREATE TABLE tips (
  id serial NOT NULL PRIMARY KEY,
  username varchar(100) NOT NULL,
  message varchar(140) NOT NULL,
  created timestamp with time zone NOT NULL DEFAULT(now()),
  updated timestamp with time zone NOT NULL DEFAULT(now())
)

