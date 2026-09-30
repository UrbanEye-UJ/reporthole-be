#!/usr/bin/env bash
# Manual backup script for Reporthole's production Postgres database.
# Not wired to any schedule or GitHub Action — run by hand after SSHing into
# the Virtarix host (or from your own cron, if you set that up separately).
# Usage: ./backup.sh [output-dir]   (defaults to /home/ubuntu/backups)

set -euo pipefail

OUT_DIR="${1:-/home/ubuntu/backups}"
mkdir -p "$OUT_DIR"

# docker-compose.prod.yml's own env vars — same ones the Postgres container was
# started with, needed here to authenticate against it.
source /home/ubuntu/.env

TIMESTAMP=$(date -u '+%Y%m%dT%H%M%SZ')
OUT_FILE="$OUT_DIR/reporthole-${TIMESTAMP}.sql.gz"

echo "=== Backing up reporthole-postgres ==="
echo "Output: $OUT_FILE"

docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" reporthole-postgres \
  pg_dump -U "$POSTGRES_USERNAME" -d reporthole \
  | gzip > "$OUT_FILE"

echo "Done: $(du -h "$OUT_FILE" | cut -f1)"
echo ""
echo "This file is still on the same disk as the database it backs up — copy it"
echo "off this host too, e.g. from your own machine:"
echo "  scp ${USER:-ubuntu}@<virtarix-host>:$OUT_FILE ./"
