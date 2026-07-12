# Flyway migrations

`mawa-bes` is the API service and does not execute database migrations.

Migrations are owned by the dedicated `mawa-flyway-runner` project. Deploy that project as the Cloud Run Job or Cloud Build migration gate before deploying or promoting `mawa-bes`.

The API project intentionally contains no Flyway runtime dependencies, migration controller, startup migration component, or migration job runner. Do not add `MAWA_FLYWAY_*` settings to the normal API service.

Deployment order:

```text
1. Build and deploy mawa-flyway-runner for the target environment.
2. Execute the migration Cloud Run Job and require exit code 0.
3. Deploy or promote mawa-bes only after migrations succeed.
```

For emergency migrations, execute the standalone runner with controlled environment settings. Do not reintroduce database migration execution into the API process.
