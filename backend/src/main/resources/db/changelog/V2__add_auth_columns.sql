-- Add password column (nullable — OAuth users have no local password)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password VARCHAR(255);

-- Add provider column to distinguish local vs google accounts
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS provider VARCHAR(20) NOT NULL DEFAULT 'local';

-- full_name alias: the JPA entity uses fullName mapped to a column.
-- If your V1 migration used "name", add a full_name column and migrate data.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);

UPDATE users SET full_name = name WHERE full_name IS NULL;

