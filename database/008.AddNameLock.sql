-- Migration 008: Add name_locked to profiles.
-- Set by the /rename and /namelock Discord commands. A locked profile cannot be deleted, so it
-- keeps its name and its slot toward max_profiles.
USE oddlabs;

ALTER TABLE profiles
  ADD COLUMN name_locked TINYINT(1) NOT NULL DEFAULT 0;
