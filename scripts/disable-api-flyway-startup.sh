#!/usr/bin/env bash
set -euo pipefail

: "${PROJECT_ID:?Set PROJECT_ID}"
: "${REGION:?Set REGION}"
: "${SERVICE_NAME:?Set SERVICE_NAME, for example mawa-bes-dev}"

gcloud run services update "${SERVICE_NAME}" \
  --project="${PROJECT_ID}" \
  --region="${REGION}" \
  --update-env-vars=MAWA_FLYWAY_STARTUP_ENABLED=false,MAWA_FLYWAY_STARTUP_MODE=disabled,MAWA_FLYWAY_JOB_ENABLED=false
