-- Rollback 003: Remove Steam identity support
USE oddlabs;

ALTER TABLE game_players
  DROP COLUMN IF EXISTS rating_delta,
  DROP COLUMN IF EXISTS result;

ALTER TABLE profiles
  DROP COLUMN IF EXISTS best_win_streak,
  DROP COLUMN IF EXISTS current_win_streak,
  DROP COLUMN IF EXISTS steam_id;

ALTER TABLE profiles
  MODIFY COLUMN reg_id INT NOT NULL;
