#!/usr/bin/env bash
set -euo pipefail

# Creates/updates a Cloud Run Job that runs MAWA Flyway migrations using the same image as the API.
# Provide the same DB/Secret Manager environment variables used by the API service.
#
# Required environment variables:
#   PROJECT_ID      e.g. mawa-162022
#   REGION          e.g. us-central1
#   IMAGE           e.g. us-central1-docker.pkg.dev/mawa-162022/mawa-repo/mawa-bes:phase-1
#
# Optional environment variables:
#   JOB_NAME        default: mawa-bes-flyway-migrations
#   SERVICE_ACCOUNT Cloud Run runtime service account
#   PROFILE         Spring profile, default: dev
#   GCP_SECRET_MAPPINGS, GCP_SECRET_ENABLED, GCP_PROJECT_ID, DB_URL, DB_USERNAME, DB_SCHEMA, etc.

PROJECT_ID="${PROJECT_ID:?PROJECT_ID is required}"
REGION="${REGION:?REGION is required}"
IMAGE="${IMAGE:?IMAGE is required}"
JOB_NAME="${JOB_NAME:-mawa-bes-flyway-migrations}"
PROFILE="${PROFILE:-dev}"

ENV_PAIRS=(
  "SPRING_PROFILES_ACTIVE=${PROFILE}"
  "MAWA_FLYWAY_STARTUP_ENABLED=false"
  "MAWA_FLYWAY_STARTUP_MODE=disabled"
  "MAWA_FLYWAY_JOB_ENABLED=true"
  "MAWA_FLYWAY_JOB_EXIT_ON_COMPLETE=true"
  "MAWA_FLYWAY_CONTINUE_ON_ERROR=false"
  "MAWA_FLYWAY_REPAIR_FAILED=${MAWA_FLYWAY_REPAIR_FAILED:-false}"
  "MAWA_FLYWAY_BASELINE_ON_MIGRATE=${MAWA_FLYWAY_BASELINE_ON_MIGRATE:-true}"
  "MAWA_FLYWAY_DEFAULT_SCHEMA_ENABLED=${MAWA_FLYWAY_DEFAULT_SCHEMA_ENABLED:-true}"
  "MAWA_FLYWAY_TENANT_SCHEMAS_ENABLED=${MAWA_FLYWAY_TENANT_SCHEMAS_ENABLED:-true}"
  "MAWA_FLYWAY_MAX_TENANT_ERRORS=${MAWA_FLYWAY_MAX_TENANT_ERRORS:-1}"
)

# Preserve common API runtime settings when supplied to this script.
for name in \
  GCP_SECRET_ENABLED GCP_PROJECT_ID GCP_SECRET_MAPPINGS \
  DB_URL DB_SCHEMA DB_USERNAME DB_PASSWORD \
  JWT_SECRET ENCRYPTION_SECRET \
  MAWA_ADMIN_API_URL MAWA_ADMIN_API_USERNAME MAWA_ADMIN_API_PASSWORD; do
  if [[ -n "${!name:-}" ]]; then
    ENV_PAIRS+=("${name}=${!name}")
  fi
done

# Use a custom delimiter so comma-separated values like GCP_SECRET_MAPPINGS remain intact.
ENV_VARS="^@^$(IFS='@'; echo "${ENV_PAIRS[*]}")"

ARGS=(
  run jobs deploy "${JOB_NAME}"
  --project "${PROJECT_ID}"
  --region "${REGION}"
  --image "${IMAGE}"
  --command java
  --args=-jar,/app.jar,--spring.main.web-application-type=none,--mawa.flyway.startup.enabled=false,--mawa.flyway.startup.mode=disabled,--mawa.flyway.job.enabled=true,--mawa.flyway.job.exit-on-complete=true,--mawa.flyway.continue-on-error=false
  --set-env-vars "${ENV_VARS}"
  --tasks 1
  --parallelism 1
  --max-retries 0
  --task-timeout "3600s"
)

if [[ -n "${SERVICE_ACCOUNT:-}" ]]; then
  ARGS+=(--service-account "${SERVICE_ACCOUNT}")
fi

gcloud "${ARGS[@]}"
