#!/usr/bin/env bash
# Restart script for Reporthole production services.
# Called by the GitHub Actions restart workflow.
# Usage: ./restart.sh <backend|ui|both|caddy|postgres|pgadmin|dozzle>
#
# "both" is the only grouped option — it restarts backend+ui together.
# Every other service is individual-only, by design (no "all").

set -euo pipefail

SERVICE="${1:-}"

declare -A SERVICE_MAP=(
  [backend]="reporthole-be"
  [ui]="reporthole-ui"
  [both]="reporthole-be reporthole-ui"
  [caddy]="reporthole-caddy"
  [postgres]="reporthole-postgres"
  [pgadmin]="reporthole-pgadmin"
  [dozzle]="reporthole-dozzle"
)

if [[ -z "$SERVICE" ]]; then
  VALID_SERVICES="${!SERVICE_MAP[*]}"
  echo "Usage: $0 <${VALID_SERVICES// /|}>"
  exit 1
fi

if [[ -z "${SERVICE_MAP[$SERVICE]:-}" ]]; then
  echo "Error: unknown service '${SERVICE}'. Must be one of: ${!SERVICE_MAP[*]}"
  exit 1
fi

CONTAINERS="${SERVICE_MAP[$SERVICE]}"

echo "=== Restarting: $CONTAINERS ==="
for container in $CONTAINERS; do
  echo "Restarting $container..."
  docker restart "$container"
  echo "$container restarted"
done

echo ""
echo "=== Container status ==="
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
