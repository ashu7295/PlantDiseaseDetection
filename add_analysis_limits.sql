-- Add analysis limit tracking columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS free_analyses_used INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS free_analyses_limit INT NOT NULL DEFAULT 10;

-- Update existing users to have the default values
UPDATE users SET free_analyses_used = 0 WHERE free_analyses_used IS NULL;
UPDATE users SET free_analyses_limit = 10 WHERE free_analyses_limit IS NULL; 