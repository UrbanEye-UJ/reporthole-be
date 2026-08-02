#!/usr/bin/env bash
# Deploy script for Reporthole production services.
# Called by the GitHub Actions deploy workflow.
# Usage: ./deploy.sh <backend|ui|both>

set -euo pipefail

# ── Argument validation ────────────────────────────────────────────────────────
SERVICE="${1:-}"

if [[ -z "$SERVICE" ]]; then
  echo "Usage: $0 <backend|ui|both>"
  exit 1
fi

if [[ "$SERVICE" != "backend" && "$SERVICE" != "ui" && "$SERVICE" != "both" ]]; then
  echo "Error: unknown service '${SERVICE}'. Must be one of: backend, ui, both"
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

if [[ "$SERVICE" == "backend" ]]; then
  SERVICES="reporthole-be"
elif [[ "$SERVICE" == "ui" ]]; then
  SERVICES="reporthole-ui"
else
  SERVICES="reporthole-be reporthole-ui"
fi

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
# --no-deps       : don't touch postgres or caddy
# --force-recreate: stop and replace existing containers regardless of which
#                   compose project originally created them (handles project-name
#                   mismatches after the compose file was moved on the VM)
$COMPOSE up -d --no-deps --force-recreate $SERVICES

# ── Health checks ──────────────────────────────────────────────────────────────
echo ""
echo "=== Waiting for health checks ==="
for svc in $SERVICES; do
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
