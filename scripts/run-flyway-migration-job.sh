#!/usr/bin/env bash
set -euo pipefail

# Run the MAWA Flyway migration job from the built application jar.
# This is intended for Cloud Run Jobs, Cloud Build, or controlled local execution.

JAR_PATH="${JAR_PATH:-/app.jar}"

exec java -jar "${JAR_PATH}" \
  --spring.main.web-application-type=none \
  --mawa.flyway.startup.enabled=false \
  --mawa.flyway.startup.mode=disabled \
  --mawa.flyway.job.enabled=true \
  --mawa.flyway.job.exit-on-complete=true \
  --mawa.flyway.continue-on-error=false
