#!/usr/bin/env bash
# Manual restore script for Reporthole's production Postgres database.
# DESTRUCTIVE: overwrites data in the "reporthole" database with the contents
# of the given dump (produced by backup.sh). Run by hand after SSHing into the
# Virtarix host. Usage: ./restore.sh <path-to-dump.sql.gz>

set -euo pipefail

DUMP_FILE="${1:-}"
if [[ -z "$DUMP_FILE" || ! -f "$DUMP_FILE" ]]; then
  echo "Usage: $0 <path-to-dump.sql.gz>"
  exit 1
fi

source /home/ubuntu/.env

echo "=== About to restore into 'reporthole' from: $DUMP_FILE ==="
echo "This overwrites existing data in that database."
read -r -p "Type 'yes' to continue: " CONFIRM
if [[ "$CONFIRM" != "yes" ]]; then
  echo "Aborted."
  exit 1
fi

gunzip -c "$DUMP_FILE" | docker exec -i -e PGPASSWORD="$POSTGRES_PASSWORD" reporthole-postgres \
  psql -U "$POSTGRES_USERNAME" -d reporthole

echo "Restore complete."
