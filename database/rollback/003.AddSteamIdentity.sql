-- Rollback script for 003.AddSteamIdentity.sql
USE oddlabs;

-- Delete all Steam user profiles (must delete profiles before registrations due to foreign key)
-- Find Steam profiles by joining to registrations
DELETE p FROM profiles p
INNER JOIN registrations r ON p.reg_id = r.id
WHERE r.steam_id IS NOT NULL;

-- Delete all Steam user registrations
DELETE FROM registrations WHERE steam_id IS NOT NULL;

-- Remove steam_id column from registrations table
ALTER TABLE registrations
    DROP COLUMN steam_id;
