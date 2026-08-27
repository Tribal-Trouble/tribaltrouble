-- Migration 006: Add banned_words table for name filtering and chat censoring.
-- match_type 'substring' rejects the word anywhere in a name (after leetspeak/separator
-- normalization) and is reserved for words with no innocent uses. 'exact' rejects only a
-- name that is nothing but the word, avoiding false positives like Hancock or Dickens.
-- The word list itself is deliberately not checked in; seed it out of band or manage it
-- with the /bannedwords Discord command. Rows take effect without a server restart.
USE oddlabs;

CREATE TABLE banned_words (
  word VARCHAR(64) NOT NULL,
  match_type VARCHAR(16) NOT NULL DEFAULT 'substring',
  PRIMARY KEY (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
