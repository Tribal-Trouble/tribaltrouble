-- Rollback for migration 005: Record the host's sim version per game
USE oddlabs;

ALTER TABLE games
  DROP COLUMN sim_version;
