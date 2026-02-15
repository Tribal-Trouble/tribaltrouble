-- Rollback script for 004.AddStreakTracking.sql
USE oddlabs;

-- Remove streak columns from profiles table
ALTER TABLE profiles
    DROP COLUMN current_win_streak,
    DROP COLUMN best_win_streak;
