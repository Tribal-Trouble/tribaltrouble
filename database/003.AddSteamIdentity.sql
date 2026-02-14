-- Select database
USE oddlabs;

-- Add steam_id to registrations table
-- Each Steam user gets their own registration for banning/admin control
-- Profiles link via reg_id (no need for redundant steam_id on profiles)
ALTER TABLE registrations
    ADD COLUMN steam_id BIGINT NULL UNIQUE;
