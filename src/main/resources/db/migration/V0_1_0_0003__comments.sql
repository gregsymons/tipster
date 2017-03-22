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

CREATE TABLE comments (
  id serial PRIMARY KEY,
  tip_id integer REFERENCES tips(id) ON DELETE CASCADE,
  username varchar(100) NOT NULL,
  comment text NOT NULL,
  created timestamp with time zone NOT NULL DEFAULT(now())
);

GRANT 
  SELECT 
ON comments
TO ReadUser;

GRANT
  SELECT,
  INSERT,
  UPDATE
ON comments
TO WriteUser;

GRANT 
  USAGE 
ON SEQUENCE comments_id_seq
TO WriteUser;
