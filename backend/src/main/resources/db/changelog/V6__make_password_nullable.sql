-- OAuth users (Google) have no password. The password column must be nullable.
-- V2 added it without a default so it defaulted to NOT NULL.
ALTER TABLE users
    ALTER COLUMN password DROP NOT NULL;

