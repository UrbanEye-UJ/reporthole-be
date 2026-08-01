-- Promotes a CIVILIAN user to ADMIN and marks their pending application as APPROVED.
--
-- Prerequisites:
--   - psql connected to the reporthole database
--   - The user UUID from the admin_applications table or users table
--
-- Usage:
--   psql -d reporthole -f scripts/promote-to-admin.sql -v user_id="'<UUID>'"
--
-- Example:
--   psql -d reporthole -f scripts/promote-to-admin.sql -v user_id="'a1b2c3d4-e5f6-7890-abcd-ef1234567890'"

BEGIN;

UPDATE users
SET user_role = 'ADMIN'
WHERE user_id = 'd9591737-88cc-41ec-b53f-9beb96368c20';

UPDATE admin_applications
SET status = 'APPROVED'
WHERE user_id = 'd9591737-88cc-41ec-b53f-9beb96368c20'
  AND status = 'PENDING';

COMMIT;
