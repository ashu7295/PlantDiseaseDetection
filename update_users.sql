-- Update all existing users to be verified so they can authenticate
UPDATE users SET is_verified = true WHERE is_verified = false;

-- Check the results
SELECT id, name, email, is_verified FROM users; 