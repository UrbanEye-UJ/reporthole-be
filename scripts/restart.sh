#!/usr/bin/env bash
# Restart script for Reporthole production services.
# Called by the GitHub Actions restart workflow.
# Usage: ./restart.sh <backend|ui|caddy|postgres|all>

set -euo pipefail

SERVICE="${1:-}"

if [[ -z "$SERVICE" ]]; then
  echo "Usage: $0 <backend|ui|caddy|postgres|all>"
  exit 1
fi

declare -A SERVICE_MAP=(
  [backend]="reporthole-be"
  [ui]="reporthole-ui"
  [caddy]="reporthole-caddy"
  [postgres]="reporthole-postgres"
  [all]="reporthole-be reporthole-ui reporthole-caddy reporthole-postgres"
)

if [[ -z "${SERVICE_MAP[$SERVICE]:-}" ]]; then
  echo "Error: unknown service '${SERVICE}'. Must be one of: backend, ui, caddy, postgres, all"
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
