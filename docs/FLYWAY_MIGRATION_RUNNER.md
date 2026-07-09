# Flyway migrations

`mawa-bes` is the API service and should not run tenant Flyway migrations during startup.

Migrations are now owned by the dedicated `mawa-flyway-runner` project. Deploy that project as the Cloud Run Job / Cloud Build migration gate.

Keep API service settings as:

```text
MAWA_FLYWAY_STARTUP_ENABLED=false
MAWA_FLYWAY_STARTUP_MODE=disabled
MAWA_FLYWAY_JOB_ENABLED=false
```

For emergency-only API-side migration, reintroduce a controlled migration runner deliberately. Do not enable Flyway startup on the normal API service.
