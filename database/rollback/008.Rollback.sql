-- Rollback 008: Remove name_locked from profiles
USE oddlabs;

ALTER TABLE profiles
  DROP COLUMN name_locked;
