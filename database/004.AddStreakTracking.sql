-- Add win streak tracking columns to profiles table
USE oddlabs;

-- Add current_win_streak and best_win_streak columns
ALTER TABLE profiles
    ADD COLUMN current_win_streak INT DEFAULT 0 NOT NULL,
    ADD COLUMN best_win_streak INT DEFAULT 0 NOT NULL;
