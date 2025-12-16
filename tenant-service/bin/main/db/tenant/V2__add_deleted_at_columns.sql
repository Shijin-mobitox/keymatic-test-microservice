-- Ensure legacy tenant databases have the deleted_at column for soft deletes
ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
