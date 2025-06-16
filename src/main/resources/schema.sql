-- Alter table to set default values for free analyses
ALTER TABLE users 
MODIFY COLUMN free_analyses_used INT NOT NULL DEFAULT 0,
MODIFY COLUMN free_analyses_limit INT NOT NULL DEFAULT 10; 