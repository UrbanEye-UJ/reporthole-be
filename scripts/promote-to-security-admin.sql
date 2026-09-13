-- Promotes a user to SECURITY_ADMIN.
--
-- This is a BOOTSTRAP-ONLY script. Granting SECURITY_ADMIN is normally done through
-- POST /admin/security/users/{userId}/role by an existing security admin, which also
-- writes an append-only access-control audit row. But the very first security admin has
-- no one above them to grant the role — hence this script.
--
-- After the first security admin exists, do NOT use this script again: use the endpoint
-- so the change is audited.
--
-- Prerequisites:
--   - psql connected to the reporthole database
--   - The user UUID from the users table
--
-- Usage:
--   psql -d reporthole -f scripts/promote-to-security-admin.sql -v user_id="'<UUID>'"
--
-- Example:
--   psql -d reporthole -f scripts/promote-to-security-admin.sql -v user_id="'a1b2c3d4-e5f6-7890-abcd-ef1234567890'"

BEGIN;

UPDATE users
SET user_role = 'SECURITY_ADMIN'
WHERE user_id = '51683a87-d2b4-4e06-a3a4-b5e36a412968';

-- Invalidate any existing sessions so the new role takes effect on next sign-in,
-- mirroring what the endpoint does via credentialsValidFrom.
UPDATE user_auth
SET auth_credentials_valid_from = date_trunc('second', now())
WHERE auth_id = '51683a87-d2b4-4e06-a3a4-b5e36a412968';

COMMIT;
