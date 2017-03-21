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

------
--
-- Create initial database users.
--
-- The users created here are primarily used for integration testing. Production
-- deploys may use different credentials (e.g. temporary credentials managed by
-- a Hashicorp Vault)
-- 
-- The names of the users can be configured in 
-- tipster.storage.migrations.flyway.placeholders, but doing so will only affect
-- an empty database.
--
------

CREATE ROLE ReadUser NOINHERIT;

CREATE ROLE WriteUser NOINHERIT;

CREATE ROLE ${initial_read_user.name} 
  LOGIN
  PASSWORD '${initial_read_user.password}'
  IN ROLE ReadUser
  INHERIT;

CREATE ROLE ${initial_write_user.name}
  LOGIN
  PASSWORD '${initial_write_user.password}'
  IN ROLE WriteUser
  INHERIT;

GRANT USAGE ON SCHEMA ${db.schema} TO ReadUser, WriteUser;

GRANT 
  SELECT 
ON ALL TABLES IN SCHEMA ${db.schema} 
TO ReadUser;

GRANT
  SELECT,
  INSERT,
  UPDATE
ON ALL TABLES IN SCHEMA ${db.schema}
TO WriteUser;

GRANT 
  USAGE 
ON ALL SEQUENCES IN SCHEMA ${db.schema}
TO WriteUser;
