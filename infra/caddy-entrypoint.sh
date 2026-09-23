#!/bin/sh
# Computes the bcrypt hash Caddy's basic_auth directive needs for the /pgadmin and /logs
# gate, at container startup, from the plaintext POSTGRES_PASSWORD already in .env — so
# there's no separate hashed secret to generate or keep in sync by hand. DEV_TOOLS_USERNAME
# is set directly in docker-compose (mapped from MAIL_USERNAME); only the password needs this
# runtime step, since Caddy will not accept a plaintext value there.
set -e

export DEV_TOOLS_PASSWORD_HASH="$(caddy hash-password --plaintext "$POSTGRES_PASSWORD")"

exec caddy run --config /etc/caddy/Caddyfile --adapter caddyfile
