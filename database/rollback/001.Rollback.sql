-- Rollback 001: Restore wide player columns on games table
USE oddlabs;

-- Re-add wide player columns to games table
ALTER TABLE games
  ADD COLUMN player1_name VARCHAR(128) DEFAULT NULL,
  ADD COLUMN player1_race VARCHAR(1) DEFAULT NULL,
  ADD COLUMN player1_team INT DEFAULT NULL,
  ADD COLUMN player2_name VARCHAR(128) DEFAULT NULL,
  ADD COLUMN player2_race VARCHAR(1) DEFAULT NULL,
  ADD COLUMN player2_team INT DEFAULT NULL,
  ADD COLUMN player3_name VARCHAR(128) DEFAULT NULL,
  ADD COLUMN player3_race VARCHAR(1) DEFAULT NULL,
  ADD COLUMN player3_team INT DEFAULT NULL,
  ADD COLUMN player4_name VARCHAR(128) DEFAULT NULL,
  ADD COLUMN player4_race VARCHAR(1) DEFAULT NULL,
  ADD COLUMN player4_team INT DEFAULT NULL,
  ADD COLUMN player5_name VARCHAR(128) DEFAULT NULL,
  ADD COLUMN player5_race VARCHAR(1) DEFAULT NULL,
  ADD COLUMN player5_team INT DEFAULT NULL,
  ADD COLUMN player6_name VARCHAR(128) DEFAULT NULL,
  ADD COLUMN player6_race VARCHAR(1) DEFAULT NULL,
  ADD COLUMN player6_team INT DEFAULT NULL,
  ADD COLUMN player7_name VARCHAR(128) DEFAULT NULL,
  ADD COLUMN player7_race VARCHAR(1) DEFAULT NULL,
  ADD COLUMN player7_team INT DEFAULT NULL,
  ADD COLUMN player8_name VARCHAR(128) DEFAULT NULL,
  ADD COLUMN player8_race VARCHAR(1) DEFAULT NULL,
  ADD COLUMN player8_team INT DEFAULT NULL;

-- Note: game_players table is NOT dropped — it is needed by migration 003 and runtime code.
