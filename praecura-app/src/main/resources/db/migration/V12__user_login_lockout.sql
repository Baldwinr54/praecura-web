-- Add login lockout fields (5 failed attempts => 15 minutes lockout)
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_users_locked_until ON users(locked_until);
