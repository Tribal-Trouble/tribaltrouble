-- Migration 007: Add OpenSkill rating storage
-- Stores per-player mu/sigma.
-- Elo continues to live in `profiles.rating`; this table is additive so the
-- existing system keeps working unchanged.
USE oddlabs;

CREATE TABLE openskill_rating (
  nick varchar(128) NOT NULL PRIMARY KEY,
  mu double NOT NULL,
  sigma double NOT NULL
);

UPDATE game_players
SET nick = CASE nick
  WHEN 'AI Easy' THEN 'Easy AI'
  WHEN 'AI Normal' THEN 'Normal AI'
  WHEN 'AI Hard' THEN 'Hard AI'
  ELSE nick
END
WHERE nick IN ('AI Easy', 'AI Normal', 'AI Hard');
