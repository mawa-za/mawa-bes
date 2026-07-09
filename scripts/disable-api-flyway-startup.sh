#!/usr/bin/env bash
set -euo pipefail

# Updates the Cloud Run API service so Flyway does not run during service startup.
# Run this once per environment after creating the dedicated migration job.

PROJECT_ID="${PROJECT_ID:?PROJECT_ID is required}"
REGION="${REGION:?REGION is required}"
SERVICE_NAME="${SERVICE_NAME:-mawa-bes-dev}"

gcloud run services update "${SERVICE_NAME}" \
  --project "${PROJECT_ID}" \
  --region "${REGION}" \
  --update-env-vars "MAWA_FLYWAY_STARTUP_ENABLED=false,MAWA_FLYWAY_STARTUP_MODE=disabled,MAWA_FLYWAY_CONTINUE_ON_ERROR=false,MAWA_FLYWAY_JOB_ENABLED=false"
