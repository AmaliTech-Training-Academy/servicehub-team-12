-- Add department_name: a plain-text label for the user's department (distinct from department_id FK).
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS department_name VARCHAR(255);

-- The full_name column added in V2 is now redundant — name is the authoritative column.
-- Copy any existing full_name data back into name for rows that may have been inserted via the old mapping.
UPDATE users SET name = full_name WHERE full_name IS NOT NULL AND (name IS NULL OR name = '');

