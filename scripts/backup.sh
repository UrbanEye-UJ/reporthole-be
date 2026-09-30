#!/usr/bin/env bash
# Backup script for Reporthole's production Postgres database.
# Run either by hand over SSH on the Virtarix host, or by the "Backup" GitHub
# Actions workflow (.github/workflows/backup.yml), which SSHes in to call this
# and then copies the result off the host as a workflow artifact — this script
# only writes to local disk on the VM itself either way.
# Usage: ./backup.sh [output-dir]   (defaults to /home/ubuntu/backups)

set -euo pipefail

OUT_DIR="${1:-/home/ubuntu/backups}"
mkdir -p "$OUT_DIR"

# docker-compose.prod.yml's own env vars — same ones the Postgres container was
# started with, needed here to authenticate against it.
source /home/ubuntu/.env

TIMESTAMP=$(date -u '+%Y%m%dT%H%M%SZ')
OUT_FILE="$OUT_DIR/reporthole-${TIMESTAMP}.sql.gz"

echo "=== Backing up reporthole-postgres ===" >&2
echo "Output: $OUT_FILE" >&2

docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" reporthole-postgres \
  pg_dump -U "$POSTGRES_USERNAME" -d reporthole \
  | gzip > "$OUT_FILE"

echo "Done: $(du -h "$OUT_FILE" | cut -f1)" >&2

# Keep the last 14 days of local dumps so a nightly schedule doesn't fill the
# disk — the off-box copy (GitHub Actions artifact, or your own scp) is the
# durable one; this is just local headroom.
find "$OUT_DIR" -name 'reporthole-*.sql.gz' -mtime +14 -delete

# Printed alone on the last stdout line (everything else above is on stderr)
# so a caller — a human, or backup.yml's "fetch the dump" step — can capture
# just the path with a single command substitution.
echo "$OUT_FILE"
