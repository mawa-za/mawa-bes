MAWA-BES FLYWAY COMPILATION FIX

Why the build failed:
FlywayConfiguration.java still imports Flyway and Apache DBCP even though those
Maven dependencies were removed when migrations moved to mawa-flyway-runner.

Important when overwriting an existing checkout:
Extracting this ZIP does not automatically delete old tracked files. Run:

  PowerShell:  .\APPLY_FLYWAY_BUILD_FIX.ps1
  Linux/macOS: ./APPLY_FLYWAY_BUILD_FIX.sh

Then verify:
  git status --short
  git grep -n -E "org\.flywaydb|org\.apache\.commons\.dbcp|FlywayConfiguration" -- "*.java"

The grep command must return no results.

Recommended commit:
  fix(build): remove orphaned Flyway API components
