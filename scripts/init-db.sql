-- Runs once on first container startup via /docker-entrypoint-initdb.d/
-- Creates the reporthole database and enables the PostGIS extension.

SELECT 'CREATE DATABASE reporthole'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'reporthole')
\gexec

\c reporthole

CREATE EXTENSION IF NOT EXISTS postgis;
