-- Select database
USE oddlabs;

-- Create the game players table
CREATE TABLE `game_players` (
  `id` bigint NOT NULL AUTO_INCREMENT KEY,
  `game_id` int NOT NULL,
  `nick` varchar(128) NOT NULL,
  `team` int NOT NULL,
  `race` varchar(1) NOT NULL
);

-- Migrate the players from the games table to the game_players table
INSERT INTO
    game_players (game_id, nick, team, race)
SELECT
    id AS GameId,
    player1_name AS Name,
    player1_team AS Team,
    player1_race AS Race
FROM games
WHERE
    player1_name IS NOT NULL AND player1_team IS NOT NULL AND player1_race IS NOT NULL
UNION ALL
SELECT
    id,
    player2_name,
    player2_team,
    player2_race
FROM games
WHERE
    player2_name IS NOT NULL AND player2_team IS NOT NULL AND player2_race IS NOT NULL
UNION ALL
SELECT
    id,
    player3_name,
    player3_team,
    player3_race
FROM games
WHERE
    player3_name IS NOT NULL AND player3_team IS NOT NULL AND player3_race IS NOT NULL
UNION ALL
SELECT
    id,
    player4_name,
    player4_team,
    player4_race
FROM games
WHERE
    player4_name IS NOT NULL AND player4_team IS NOT NULL AND player4_race IS NOT NULL
UNION ALL
SELECT
    id,
    player5_name,
    player5_team,
    player5_race
FROM games
WHERE
    player5_name IS NOT NULL AND player5_team IS NOT NULL AND player5_race IS NOT NULL
UNION ALL
SELECT
    id,
    player6_name,
    player6_team,
    player6_race
FROM games
WHERE
    player6_name IS NOT NULL AND player6_team IS NOT NULL AND player6_race IS NOT NULL
UNION ALL
SELECT
    id,
    player7_name,
    player7_team,
    player7_race
FROM games
WHERE
    player7_name IS NOT NULL AND player7_team IS NOT NULL AND player7_race IS NOT NULL
UNION ALL
SELECT
    id,
    player8_name,
    player8_team,
    player8_race
FROM games
WHERE
    player8_name IS NOT NULL AND player8_team IS NOT NULL AND player8_race IS NOT NULL;

ALTER TABLE games
    DROP COLUMN player1_name,
    DROP COLUMN player1_team,
    DROP COLUMN player1_race,
    DROP COLUMN player2_name,
    DROP COLUMN player2_team,
    DROP COLUMN player2_race,
    DROP COLUMN player3_name,
    DROP COLUMN player3_team,
    DROP COLUMN player3_race,
    DROP COLUMN player4_name,
    DROP COLUMN player4_team,
    DROP COLUMN player4_race,
    DROP COLUMN player5_name,
    DROP COLUMN player5_team,
    DROP COLUMN player5_race,
    DROP COLUMN player6_name,
    DROP COLUMN player6_team,
    DROP COLUMN player6_race,
    DROP COLUMN player7_name,
    DROP COLUMN player7_team,
    DROP COLUMN player7_race,
    DROP COLUMN player8_name,
    DROP COLUMN player8_team,
    DROP COLUMN player8_race;
