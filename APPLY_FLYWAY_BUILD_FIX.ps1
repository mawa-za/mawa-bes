$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$obsoleteFiles = @(
  'src/main/java/za/co/mawa/bes/configuration/flyway/FlywayConfiguration.java',
  'src/main/java/za/co/mawa/bes/configuration/flyway/FlywayMigrationJobRunner.java',
  'src/main/java/za/co/mawa/bes/controller/v2/FlywayMigrationControllerV2.java',
  'cloudbuild.migration-job.yaml',
  'deploy/cloud-run/mawa-bes-migration-job-env.example',
  'scripts/deploy-flyway-migration-job.sh',
  'scripts/run-flyway-migration-job.sh'
)

foreach ($file in $obsoleteFiles) {
  if (Test-Path -LiteralPath $file) {
    Remove-Item -LiteralPath $file -Force
    Write-Host "Removed $file"
  }
}

if (Get-Command git -ErrorAction SilentlyContinue) {
  git add -A -- $obsoleteFiles
  if ($LASTEXITCODE -ne 0) { throw 'git add failed' }
}

$forbidden = Get-ChildItem 'src/main/java' -Recurse -Filter '*.java' |
  Select-String -Pattern 'org\.flywaydb|org\.apache\.commons\.dbcp|FlywayConfiguration|FlywayMigrationJobRunner|FlywayMigrationControllerV2'

if ($forbidden) {
  $forbidden | ForEach-Object { Write-Host $_ }
  throw 'Obsolete Flyway API references still exist.'
}

Write-Host 'Flyway API cleanup applied successfully.'
Write-Host 'Next: git status, commit, push phase-1, then rerun Cloud Build.'
