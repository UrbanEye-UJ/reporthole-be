#!/usr/bin/env bash
# Deploy script for Reporthole production services.
# Called by the GitHub Actions deploy workflow.
# Usage: ./deploy.sh <backend|ui|both|postgres|caddy|pgadmin|dozzle>
#
# "both" is the only grouped option — it redeploys backend+ui together.
# Every other service is individual-only, by design (no "all").

set -euo pipefail

# ── Argument validation ────────────────────────────────────────────────────────
SERVICE="${1:-}"

# Two maps because compose service names and container_names diverge for the
# image-only services: backend/ui happen to share the same string as their
# container_name, but postgres/caddy/pgadmin/dozzle do not (e.g. compose service
# "pgadmin" -> container_name "reporthole-pgadmin"). `docker compose build/up/logs`
# need the former; `docker inspect` in the health-check loop needs the latter.
declare -A COMPOSE_SERVICE_MAP=(
  [backend]="reporthole-be"
  [ui]="reporthole-ui"
  [both]="reporthole-be reporthole-ui"
  [postgres]="postgres"
  [caddy]="caddy"
  [pgadmin]="pgadmin"
  [dozzle]="dozzle"
)

declare -A CONTAINER_MAP=(
  [backend]="reporthole-be"
  [ui]="reporthole-ui"
  [both]="reporthole-be reporthole-ui"
  [postgres]="reporthole-postgres"
  [caddy]="reporthole-caddy"
  [pgadmin]="reporthole-pgadmin"
  [dozzle]="reporthole-dozzle"
)

if [[ -z "$SERVICE" ]]; then
  VALID_SERVICES="${!CONTAINER_MAP[*]}"
  echo "Usage: $0 <${VALID_SERVICES// /|}>"
  exit 1
fi

if [[ -z "${CONTAINER_MAP[$SERVICE]:-}" ]]; then
  echo "Error: unknown service '${SERVICE}'. Must be one of: ${!CONTAINER_MAP[*]}"
  exit 1
fi

# ── Environment diagnostics ────────────────────────────────────────────────────
echo "=== Deploy triggered ==="
echo "Service(s) selected : ${SERVICE}"
echo "Running as          : $(whoami)"
echo "Host                : $(hostname)"
echo "Timestamp           : $(date -u '+%Y-%m-%dT%H:%M:%SZ')"

echo ""
echo "=== Docker version ==="
docker --version
docker compose version

echo ""
echo "=== Running containers ==="
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# ── Determine compose service name(s) ─────────────────────────────────────────
COMPOSE="docker compose -f /home/ubuntu/docker-compose.prod.yml"

SERVICES="${COMPOSE_SERVICE_MAP[$SERVICE]}"
CONTAINERS="${CONTAINER_MAP[$SERVICE]}"

# ── Pull latest code ───────────────────────────────────────────────────────────
# The workflow already pulled reporthole-be before calling this script.
# Only pull reporthole-ui if the ui is being deployed.
echo ""
echo "=== Pulling latest code ==="
if [[ "$SERVICE" == "ui" || "$SERVICE" == "both" ]]; then
  cd /home/ubuntu/reporthole-ui && git pull && cd /home/ubuntu
fi

cd /home/ubuntu

# ── Build ──────────────────────────────────────────────────────────────────────
echo ""
echo "=== Building: $SERVICES ==="
$COMPOSE build $SERVICES

# ── Start ──────────────────────────────────────────────────────────────────────
echo ""
echo "=== Starting: $SERVICES ==="
# --no-deps       : only recreate the selected service(s), never cascade to
#                   whatever they depend_on (e.g. deploying "backend" alone
#                   must not also touch postgres)
# --force-recreate: stop and replace existing containers regardless of which
#                   compose project originally created them (handles project-name
#                   mismatches after the compose file was moved on the VM)
$COMPOSE up -d --no-deps --force-recreate $SERVICES

# ── Health checks ──────────────────────────────────────────────────────────────
# pgAdmin/Dozzle have no healthcheck: block in the compose file, so these two
# always hit the 120s timeout and print a WARNING below — non-fatal, expected.
echo ""
echo "=== Waiting for health checks ==="
for svc in $CONTAINERS; do
  echo "Waiting for $svc..."
  timeout 120 bash -c \
    "until docker inspect --format='{{.State.Health.Status}}' $svc 2>/dev/null | grep -q healthy; \
     do sleep 5; done" \
  && echo "$svc is healthy" \
  || echo "WARNING: $svc did not reach healthy state within 120s — check logs below"
done

# ── Recent logs ────────────────────────────────────────────────────────────────
echo ""
echo "=== Recent logs ==="
$COMPOSE logs --tail=50 $SERVICES
