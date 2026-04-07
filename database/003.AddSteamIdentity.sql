-- Migration 003: Add Steam identity support
-- Steam users get an auto-created profile keyed by steam_id.
-- The existing profiles table continues to hold wins/losses/rating for everyone.
USE oddlabs;

-- Allow profiles to exist without a registration (Steam users won't have one)
ALTER TABLE profiles
  MODIFY COLUMN reg_id INT NULL;

-- Add steam_id to profiles so we can look up a profile by Steam account ID.
-- Only Steam-created profiles will have this set; legacy profiles keep it NULL.
ALTER TABLE profiles
  ADD COLUMN steam_id BIGINT NULL UNIQUE AFTER reg_id;

-- Win streak tracking for Steam achievements
ALTER TABLE profiles
  ADD COLUMN current_win_streak INT NOT NULL DEFAULT 0,
  ADD COLUMN best_win_streak INT NOT NULL DEFAULT 0;

-- Track per-player outcome on each game for richer history queries.
ALTER TABLE game_players
  ADD COLUMN result VARCHAR(1) NULL,
  ADD COLUMN rating_delta INT DEFAULT 0;
