-- Readd the player columns to the games table
ALTER TABLE games
ADD COLUMN player1_name varchar(128),
ADD COLUMN player1_team int,
ADD COLUMN player1_race varchar(1),
ADD COLUMN player2_name varchar(128),
ADD COLUMN player2_team int,
ADD COLUMN player2_race varchar(1),
ADD COLUMN player3_name varchar(128),
ADD COLUMN player3_team int,
ADD COLUMN player3_race varchar(1),
ADD COLUMN player4_name varchar(128),
ADD COLUMN player4_team int,
ADD COLUMN player4_race varchar(1),
ADD COLUMN player5_name varchar(128),
ADD COLUMN player5_team int,
ADD COLUMN player5_race varchar(1),
ADD COLUMN player6_name varchar(128),
ADD COLUMN player6_team int,
ADD COLUMN player6_race varchar(1),
ADD COLUMN player7_name varchar(128),
ADD COLUMN player7_team int,
ADD COLUMN player7_race varchar(1),
ADD COLUMN player8_name varchar(128),
ADD COLUMN player8_team int,
ADD COLUMN player8_race varchar(1);

-- Restore player data from game_players to games table
UPDATE games g
    LEFT JOIN (
        SELECT * FROM game_players WHERE nick IS NOT NULL AND team IS NOT NULL AND race IS NOT NULL
    ) gp ON g.id = gp.game_id
SET
    g.player1_name = (SELECT nick FROM game_players WHERE game_id = g.id AND team = 0 LIMIT 1),
    g.player1_team = 0,
    g.player1_race = (SELECT race FROM game_players WHERE game_id = g.id AND team = 0 LIMIT 1),
    g.player2_name = (SELECT nick FROM game_players WHERE game_id = g.id AND team = 1 LIMIT 1),
    g.player2_team = 1,
    g.player2_race = (SELECT race FROM game_players WHERE game_id = g.id AND team = 1 LIMIT 1),
    g.player3_name = (SELECT nick FROM game_players WHERE game_id = g.id AND team = 2 LIMIT 1),
    g.player3_team = 2,
    g.player3_race = (SELECT race FROM game_players WHERE game_id = g.id AND team = 2 LIMIT 1),
    g.player4_name = (SELECT nick FROM game_players WHERE game_id = g.id AND team = 3 LIMIT 1),
    g.player4_team = 3,
    g.player4_race = (SELECT race FROM game_players WHERE game_id = g.id AND team = 3 LIMIT 1),
    g.player5_name = (SELECT nick FROM game_players WHERE game_id = g.id AND team = 4 LIMIT 1),
    g.player5_team = 4,
    g.player5_race = (SELECT race FROM game_players WHERE game_id = g.id AND team = 4 LIMIT 1),
    g.player6_name = (SELECT nick FROM game_players WHERE game_id = g.id AND team = 5 LIMIT 1),
    g.player6_team = 5,
    g.player6_race = (SELECT race FROM game_players WHERE game_id = g.id AND team = 5 LIMIT 1),
    g.player7_name = (SELECT nick FROM game_players WHERE game_id = g.id AND team = 6 LIMIT 1),
    g.player7_team = 6,
    g.player7_race = (SELECT race FROM game_players WHERE game_id = g.id AND team = 6 LIMIT 1),
    g.player8_name = (SELECT nick FROM game_players WHERE game_id = g.id AND team = 7 LIMIT 1),
    g.player8_team = 7,
    g.player8_race = (SELECT race FROM game_players WHERE game_id = g.id AND team = 7 LIMIT 1);



-- Drop the game_players table
DROP TABLE IF EXISTS game_players;