-- Rollback 004: Restore wide game_reports table from normalized game_report_teams
USE oddlabs;

CREATE TABLE IF NOT EXISTS game_reports (
  game_id INT DEFAULT NULL,
  tick INT DEFAULT NULL,
  team1 INT DEFAULT NULL,
  team2 INT DEFAULT NULL,
  team3 INT DEFAULT NULL,
  team4 INT DEFAULT NULL,
  team5 INT DEFAULT NULL,
  team6 INT DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Pivot normalized data back into wide columns
INSERT INTO game_reports (game_id, tick, team1, team2, team3, team4, team5, team6)
SELECT
  game_id,
  tick,
  MAX(CASE WHEN team_index = 0 THEN score END),
  MAX(CASE WHEN team_index = 1 THEN score END),
  MAX(CASE WHEN team_index = 2 THEN score END),
  MAX(CASE WHEN team_index = 3 THEN score END),
  MAX(CASE WHEN team_index = 4 THEN score END),
  MAX(CASE WHEN team_index = 5 THEN score END)
FROM game_report_teams
GROUP BY game_id, tick;

DROP TABLE IF EXISTS game_report_teams;
