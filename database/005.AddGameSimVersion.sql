-- Migration 005: Record the host's sim version per game
-- 0 means legacy: hosted by a client that predates sim version reporting,
-- or recorded before this migration.
USE oddlabs;

ALTER TABLE games
  ADD COLUMN sim_version INT NOT NULL DEFAULT 0;
