-- Migration 001: Normalize game schema for dynamic player counts
-- Creates game_players table (normalized player data per game).
-- Drops unused wide player columns from games table.
USE oddlabs;

-- Create normalized game_players table
-- (game_players is already referenced by code and migration 003, but was never formally created)
CREATE TABLE IF NOT EXISTS game_players (
  id INT NOT NULL AUTO_INCREMENT,
  game_id INT NOT NULL,
  nick VARCHAR(128) NOT NULL,
  team INT NOT NULL,
  race INT NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_game_players_game_id (game_id),
  CONSTRAINT fk_game_players_game FOREIGN KEY (game_id) REFERENCES games(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Migrate existing player data from wide columns into game_players
-- (only for rows that haven't already been migrated)
INSERT INTO game_players (game_id, nick, team, race)
SELECT id, player1_name, COALESCE(player1_team, 0), COALESCE(player1_race, 0)
FROM games WHERE player1_name IS NOT NULL
AND id NOT IN (SELECT DISTINCT game_id FROM game_players);

INSERT INTO game_players (game_id, nick, team, race)
SELECT id, player2_name, COALESCE(player2_team, 1), COALESCE(player2_race, 0)
FROM games WHERE player2_name IS NOT NULL
AND id NOT IN (SELECT DISTINCT game_id FROM game_players);

INSERT INTO game_players (game_id, nick, team, race)
SELECT id, player3_name, COALESCE(player3_team, 2), COALESCE(player3_race, 0)
FROM games WHERE player3_name IS NOT NULL
AND id NOT IN (SELECT DISTINCT game_id FROM game_players);

INSERT INTO game_players (game_id, nick, team, race)
SELECT id, player4_name, COALESCE(player4_team, 3), COALESCE(player4_race, 0)
FROM games WHERE player4_name IS NOT NULL
AND id NOT IN (SELECT DISTINCT game_id FROM game_players);

INSERT INTO game_players (game_id, nick, team, race)
SELECT id, player5_name, COALESCE(player5_team, 4), COALESCE(player5_race, 0)
FROM games WHERE player5_name IS NOT NULL
AND id NOT IN (SELECT DISTINCT game_id FROM game_players);

INSERT INTO game_players (game_id, nick, team, race)
SELECT id, player6_name, COALESCE(player6_team, 5), COALESCE(player6_race, 0)
FROM games WHERE player6_name IS NOT NULL
AND id NOT IN (SELECT DISTINCT game_id FROM game_players);

INSERT INTO game_players (game_id, nick, team, race)
SELECT id, player7_name, COALESCE(player7_team, 6), COALESCE(player7_race, 0)
FROM games WHERE player7_name IS NOT NULL
AND id NOT IN (SELECT DISTINCT game_id FROM game_players);

INSERT INTO game_players (game_id, nick, team, race)
SELECT id, player8_name, COALESCE(player8_team, 7), COALESCE(player8_race, 0)
FROM games WHERE player8_name IS NOT NULL
AND id NOT IN (SELECT DISTINCT game_id FROM game_players);

-- Drop wide player columns from games table
ALTER TABLE games
  DROP COLUMN player1_name, DROP COLUMN player1_race, DROP COLUMN player1_team,
  DROP COLUMN player2_name, DROP COLUMN player2_race, DROP COLUMN player2_team,
  DROP COLUMN player3_name, DROP COLUMN player3_race, DROP COLUMN player3_team,
  DROP COLUMN player4_name, DROP COLUMN player4_race, DROP COLUMN player4_team,
  DROP COLUMN player5_name, DROP COLUMN player5_race, DROP COLUMN player5_team,
  DROP COLUMN player6_name, DROP COLUMN player6_race, DROP COLUMN player6_team,
  DROP COLUMN player7_name, DROP COLUMN player7_race, DROP COLUMN player7_team,
  DROP COLUMN player8_name, DROP COLUMN player8_race, DROP COLUMN player8_team;
