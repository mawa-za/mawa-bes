# MAWA Flyway Migration Runner

The custom Flyway runner has two responsibilities:

1. Give operators a controlled way to run default-schema and tenant-schema migrations.
2. Prevent normal API startup from being blocked by long-running tenant migrations.

## Long-term default

The long-term Cloud Run setup is now implemented:

| Runtime | Behaviour |
| --- | --- |
| API service | `MAWA_FLYWAY_STARTUP_ENABLED=false`, `MAWA_FLYWAY_STARTUP_MODE=disabled`. The API starts quickly and does not run migrations during readiness/startup. |
| Migration Cloud Run Job | `MAWA_FLYWAY_JOB_ENABLED=true`. The same image runs migrations in blocking mode, then exits with code `0` on success or `1` on failure. |

See `docs/FLYWAY_LONG_TERM_CLOUD_RUN_SETUP.md` for the Cloud Run commands and scripts.

## API-side modes

The API still supports controlled modes for emergencies or manual operation:

| Mode | Configuration | Use case |
| --- | --- | --- |
| Disabled | `MAWA_FLYWAY_STARTUP_ENABLED=false` or `MAWA_FLYWAY_STARTUP_MODE=disabled` | Normal API service default. |
| Async | `MAWA_FLYWAY_STARTUP_ENABLED=true`, `MAWA_FLYWAY_STARTUP_MODE=async` | Emergency fallback only; starts API first and runs migrations in background. |
| Blocking | `MAWA_FLYWAY_STARTUP_ENABLED=true`, `MAWA_FLYWAY_STARTUP_MODE=blocking` | Not recommended for API services; use the dedicated job instead. |

## Properties

```properties
mawa.flyway.startup.enabled=${MAWA_FLYWAY_STARTUP_ENABLED:false}
mawa.flyway.startup.mode=${MAWA_FLYWAY_STARTUP_MODE:disabled}
mawa.flyway.default-schema.enabled=${MAWA_FLYWAY_DEFAULT_SCHEMA_ENABLED:true}
mawa.flyway.tenant-schemas.enabled=${MAWA_FLYWAY_TENANT_SCHEMAS_ENABLED:true}
mawa.flyway.continue-on-error=${MAWA_FLYWAY_CONTINUE_ON_ERROR:false}
mawa.flyway.repair-failed=${MAWA_FLYWAY_REPAIR_FAILED:false}
mawa.flyway.baseline-on-migrate=${MAWA_FLYWAY_BASELINE_ON_MIGRATE:true}
mawa.flyway.max-tenant-errors=${MAWA_FLYWAY_MAX_TENANT_ERRORS:0}
mawa.flyway.job.enabled=${MAWA_FLYWAY_JOB_ENABLED:false}
mawa.flyway.job.exit-on-complete=${MAWA_FLYWAY_JOB_EXIT_ON_COMPLETE:true}
```

`MAWA_FLYWAY_MAX_TENANT_ERRORS=0` means unlimited tenant failures when continue-on-error is enabled. For the dedicated migration job, use `MAWA_FLYWAY_CONTINUE_ON_ERROR=false` or set `MAWA_FLYWAY_MAX_TENANT_ERRORS=1` so the job fails fast.

## Runtime API endpoints

```text
GET  /v2/flyway/status
POST /v2/flyway/run-now
POST /v2/flyway/run-now?blocking=true
```

These endpoints remain available for operators, but normal deployments should use the Cloud Run Job.

## Repair guidance

Use `MAWA_FLYWAY_REPAIR_FAILED=true` only deliberately after checking `flyway_schema_history`. Do not leave repair permanently enabled.

## Cloud Run Job web context note

For Cloud Run Jobs, run the normal Spring web context but set `MAWA_SCHEDULER_ENABLED=false`. This keeps security/controllers available while preventing scheduled background workers from querying tenant tables before Flyway has upgraded them. The job exits after Flyway because `MAWA_FLYWAY_JOB_EXIT_ON_COMPLETE=true`.
