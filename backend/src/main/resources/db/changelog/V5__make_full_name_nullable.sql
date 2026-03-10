-- full_name was added in V2 as an alias for name.
-- The JPA entity now maps fullName -> name (the original V1 column).
-- full_name is no longer written to, so drop the NOT NULL constraint.
ALTER TABLE users
    ALTER COLUMN full_name DROP NOT NULL;

