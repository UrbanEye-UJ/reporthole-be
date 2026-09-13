#!/usr/bin/env bash
# Promotes a user to SECURITY_ADMIN directly against the Postgres database — works against
# either the production VM or a local docker-compose.local.yml stack, same script either way.
#
# It talks to the reporthole-postgres container via `docker exec` (that container name is
# shared by docker-compose.local.yml and docker-compose.prod.yml), using whichever
# POSTGRES_USERNAME / POSTGRES_PASSWORD that stack's own .env already defines. Safe to re-run
# for different accounts — nothing here is hardcoded to one user, unlike
# scripts/promote-to-security-admin.sql.
#
# This is the BOOTSTRAP path for identity: normally SECURITY_ADMIN is granted through
#   POST /admin/security/users/{userId}/role
# by an existing security admin, which also writes an append-only access-control audit row.
# Use this script only when no security admin exists yet, or the API is unreachable — every
# other grant should go through the endpoint so it ends up on the audit trail.
#
# Usage:
#   ./promote-to-security-admin.sh <user-uuid>   promote that account
#   ./promote-to-security-admin.sh --list        list every account (id, role, status, created)
#
# Finding a UUID without --list: it's `data.userId` in a POST /auth/login response, or the
# `sub` claim of that user's JWT. Email can't be queried directly — it's AES-encrypted at rest.
#
# Env overrides:
#   ENV_FILE            .env to load POSTGRES_USERNAME/PASSWORD from. Auto-detected if unset:
#                        tries /home/ubuntu/.env (prod VM layout), then <this script>/../.env
#                        (reporthole-be/.env — local dev layout), first match wins.
#   POSTGRES_CONTAINER   defaults to reporthole-postgres

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTAINER="${POSTGRES_CONTAINER:-reporthole-postgres}"
DB_NAME="reporthole"

if [[ -n "${ENV_FILE:-}" ]]; then
  CANDIDATE_ENV_FILES=("$ENV_FILE")
else
  CANDIDATE_ENV_FILES=(
    "/home/ubuntu/.env"            # production VM: what docker-compose.prod.yml reads
    "${SCRIPT_DIR}/../.env"        # local dev: reporthole-be/.env, what docker-compose.local.yml reads
  )
fi

ENV_FILE_USED=""
for candidate in "${CANDIDATE_ENV_FILES[@]}"; do
  if [[ -f "$candidate" ]]; then
    ENV_FILE_USED="$candidate"
    break
  fi
done

if [[ -n "$ENV_FILE_USED" ]]; then
  echo "Loading Postgres credentials from ${ENV_FILE_USED}"
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE_USED"
  set +a
else
  echo "No .env found in the usual places (${CANDIDATE_ENV_FILES[*]}) — relying on already-exported env vars." >&2
fi

: "${POSTGRES_USERNAME:?POSTGRES_USERNAME not set — export it, or set ENV_FILE to the .env your docker-compose stack uses}"
[[ -n "${POSTGRES_PASSWORD:-}" ]] && export PGPASSWORD="$POSTGRES_PASSWORD"

if ! docker inspect "$CONTAINER" >/dev/null 2>&1; then
  echo "Error: container '${CONTAINER}' not found or not running. Is this the right VM?" >&2
  exit 1
fi

psql_exec() {
  docker exec -i "$CONTAINER" psql -U "$POSTGRES_USERNAME" -d "$DB_NAME" -v ON_ERROR_STOP=1 "$@"
}

ACTION="${1:-}"

if [[ "$ACTION" == "--list" || "$ACTION" == "-l" ]]; then
  echo "=== Accounts (email is encrypted at rest and not shown) ==="
  psql_exec -c "
    SELECT u.user_id, u.user_role, a.auth_status, u.user_created_at
    FROM users u
    JOIN user_auth a ON a.auth_id = u.user_id
    ORDER BY u.user_created_at;"
  exit 0
fi

USER_ID="$ACTION"

if [[ -z "$USER_ID" ]]; then
  echo "Usage: $0 <user-uuid> | --list" >&2
  exit 1
fi

UUID_RE='^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
if [[ ! "$USER_ID" =~ $UUID_RE ]]; then
  echo "Error: '${USER_ID}' doesn't look like a UUID. Run '$0 --list' to find one." >&2
  exit 1
fi

CURRENT_ROLE=$(psql_exec -tAc "SELECT user_role FROM users WHERE user_id = '${USER_ID}';" | tr -d '[:space:]')

if [[ -z "$CURRENT_ROLE" ]]; then
  echo "Error: no user found with id ${USER_ID}. Run '$0 --list' to find one." >&2
  exit 1
fi

echo "Found user ${USER_ID} — current role: ${CURRENT_ROLE}"

if [[ "$CURRENT_ROLE" == "SECURITY_ADMIN" ]]; then
  echo "This account is already SECURITY_ADMIN."
  read -r -p "Still bump their credentialsValidFrom to force a fresh login? [y/N] " CONFIRM
else
  read -r -p "Promote ${USER_ID} from ${CURRENT_ROLE} to SECURITY_ADMIN? This invalidates their current session — they must sign in again. [y/N] " CONFIRM
fi

if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
  echo "Aborted."
  exit 0
fi

psql_exec <<SQL
BEGIN;

UPDATE users
SET user_role = 'SECURITY_ADMIN'
WHERE user_id = '${USER_ID}';

-- Invalidate any existing sessions so the new role takes effect on next sign-in,
-- mirroring what POST /admin/security/users/{userId}/role does via credentialsValidFrom.
UPDATE user_auth
SET auth_credentials_valid_from = date_trunc('second', now())
WHERE auth_id = '${USER_ID}';

COMMIT;
SQL

echo
echo "Done. ${USER_ID} is now SECURITY_ADMIN and must log out/in to get a token with the new role."
