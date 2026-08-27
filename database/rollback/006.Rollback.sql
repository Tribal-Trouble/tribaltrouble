-- Rollback 006: Remove banned_words table
USE oddlabs;

DROP TABLE IF EXISTS banned_words;
