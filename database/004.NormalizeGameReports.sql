-- Migration 004: Normalize game_reports for dynamic team counts
-- Replaces wide team1-team6 columns with a normalized game_report_teams table.
USE oddlabs;

CREATE TABLE IF NOT EXISTS game_report_teams (
  id INT NOT NULL AUTO_INCREMENT,
  game_id INT NOT NULL,
  tick INT NOT NULL,
  team_index INT NOT NULL,
  score INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_grt_game_tick (game_id, tick)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Migrate existing data from wide columns into normalized rows
INSERT INTO game_report_teams (game_id, tick, team_index, score)
SELECT game_id, tick, 0, COALESCE(team1, 0) FROM game_reports WHERE team1 IS NOT NULL;

INSERT INTO game_report_teams (game_id, tick, team_index, score)
SELECT game_id, tick, 1, COALESCE(team2, 0) FROM game_reports WHERE team2 IS NOT NULL;

INSERT INTO game_report_teams (game_id, tick, team_index, score)
SELECT game_id, tick, 2, COALESCE(team3, 0) FROM game_reports WHERE team3 IS NOT NULL;

INSERT INTO game_report_teams (game_id, tick, team_index, score)
SELECT game_id, tick, 3, COALESCE(team4, 0) FROM game_reports WHERE team4 IS NOT NULL;

INSERT INTO game_report_teams (game_id, tick, team_index, score)
SELECT game_id, tick, 4, COALESCE(team5, 0) FROM game_reports WHERE team5 IS NOT NULL;

INSERT INTO game_report_teams (game_id, tick, team_index, score)
SELECT game_id, tick, 5, COALESCE(team6, 0) FROM game_reports WHERE team6 IS NOT NULL;

-- Drop the old wide table
DROP TABLE IF EXISTS game_reports;
