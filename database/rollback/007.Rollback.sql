USE oddlabs;

UPDATE game_players
SET nick = CASE nick
  WHEN 'Easy AI' THEN 'AI Easy'
  WHEN 'Normal AI' THEN 'AI Normal'
  WHEN 'Hard AI' THEN 'AI Hard'
  ELSE nick
END
WHERE nick IN ('Easy AI', 'Normal AI', 'Hard AI');

DROP TABLE openskill_rating;
