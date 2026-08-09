#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

obsolete_files=(
  'src/main/java/za/co/mawa/bes/configuration/flyway/FlywayConfiguration.java'
  'src/main/java/za/co/mawa/bes/configuration/flyway/FlywayMigrationJobRunner.java'
  'src/main/java/za/co/mawa/bes/controller/v2/FlywayMigrationControllerV2.java'
  'cloudbuild.migration-job.yaml'
  'deploy/cloud-run/mawa-bes-migration-job-env.example'
  'scripts/deploy-flyway-migration-job.sh'
  'scripts/run-flyway-migration-job.sh'
)

rm -f "${obsolete_files[@]}"
if command -v git >/dev/null 2>&1; then
  git add -A -- "${obsolete_files[@]}"
fi

if grep -RInE --include='*.java' \
  'org\.flywaydb|org\.apache\.commons\.dbcp|FlywayConfiguration|FlywayMigrationJobRunner|FlywayMigrationControllerV2' \
  src/main/java; then
  echo 'Obsolete Flyway API references still exist.' >&2
  exit 1
fi

echo 'Flyway API cleanup applied successfully.'
echo 'Next: git status, commit, push phase-1, then rerun Cloud Build.'
