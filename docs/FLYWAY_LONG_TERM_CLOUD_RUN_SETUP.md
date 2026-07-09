# MAWA Flyway long-term Cloud Run setup

The API service must not run all Flyway migrations during normal startup. Tenant schemas can take long enough to make Cloud Run startup probes fail. The long-term pattern is:

1. Build one `mawa-bes` image.
2. Run database migrations with a dedicated Cloud Run Job using that image.
3. Deploy or update the API Cloud Run service with Flyway startup disabled.

## API service configuration

Set these environment variables on every API Cloud Run service:

```text
MAWA_FLYWAY_STARTUP_ENABLED=false
MAWA_FLYWAY_STARTUP_MODE=disabled
MAWA_FLYWAY_CONTINUE_ON_ERROR=false
MAWA_FLYWAY_JOB_ENABLED=false
```

These are also now the profile defaults in `application-dev.properties`, `application-prod.properties`, and the other environment profiles. Keeping the variables on Cloud Run is still recommended so the runtime behaviour is explicit.

## Migration job configuration

The dedicated job uses the same container image as the API, but runs the application with no web server and exits after Flyway finishes.

Use this command/args on the Cloud Run Job:

```text
command: java
args: -jar,/app.jar,--spring.main.web-application-type=none,--mawa.flyway.startup.enabled=false,--mawa.flyway.startup.mode=disabled,--mawa.flyway.job.enabled=true,--mawa.flyway.job.exit-on-complete=true,--mawa.flyway.continue-on-error=false
```

Recommended environment variables for the job:

```text
SPRING_PROFILES_ACTIVE=dev
MAWA_FLYWAY_STARTUP_ENABLED=false
MAWA_FLYWAY_STARTUP_MODE=disabled
MAWA_FLYWAY_JOB_ENABLED=true
MAWA_FLYWAY_JOB_EXIT_ON_COMPLETE=true
MAWA_FLYWAY_CONTINUE_ON_ERROR=false
MAWA_FLYWAY_REPAIR_FAILED=false
MAWA_FLYWAY_BASELINE_ON_MIGRATE=true
MAWA_FLYWAY_DEFAULT_SCHEMA_ENABLED=true
MAWA_FLYWAY_TENANT_SCHEMAS_ENABLED=true
MAWA_FLYWAY_MAX_TENANT_ERRORS=1
```

The job also needs the same database and Secret Manager configuration as the API service, for example `GCP_SECRET_ENABLED`, `GCP_PROJECT_ID`, `GCP_SECRET_MAPPINGS`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `ENCRYPTION_SECRET`, depending on the environment.

## One-time setup commands

Disable API-side migration startup:

```bash
PROJECT_ID=mawa-162022 \
REGION=us-central1 \
SERVICE_NAME=mawa-bes-dev \
./scripts/disable-api-flyway-startup.sh
```

Create or update the migration job:

```bash
PROJECT_ID=mawa-162022 \
REGION=us-central1 \
JOB_NAME=mawa-bes-dev-migrations \
PROFILE=dev \
IMAGE=us-central1-docker.pkg.dev/mawa-162022/mawa-repo/mawa-bes:phase-1 \
GCP_SECRET_ENABLED=true \
GCP_PROJECT_ID=mawa-162022 \
GCP_SECRET_MAPPINGS='...' \
./scripts/deploy-flyway-migration-job.sh
```

Run the job before deploying or routing traffic to a new API revision:

```bash
gcloud run jobs execute mawa-bes-dev-migrations \
  --project mawa-162022 \
  --region us-central1 \
  --wait
```

If the job exits with a non-zero status, do not promote the API revision. Check the job logs and the affected `flyway_schema_history` row first.

## Optional Cloud Build flow

`cloudbuild.migration-job.yaml` provides an optional gated pipeline:

1. Build image.
2. Push image.
3. Deploy/update the migration job.
4. Execute the migration job and wait for it to finish.

Use it as a separate migration trigger, or place it before the Cloud Run service deployment step in a release pipeline.

## Emergency fallback

For a one-off emergency where the API service must run migrations itself, set:

```text
MAWA_FLYWAY_STARTUP_ENABLED=true
MAWA_FLYWAY_STARTUP_MODE=async
MAWA_FLYWAY_CONTINUE_ON_ERROR=true
```

Do not leave this as the permanent Cloud Run service configuration.

## Non-web migration job startup

The migration job is intended to run with:

```text
SPRING_MAIN_WEB_APPLICATION_TYPE=none
```

This prevents the job from exposing API endpoints while it performs database migration work. Servlet-only configuration such as Web MVC, request interceptors, JWT filters, and servlet security must therefore only load when the application is running as a servlet web application. The backend marks those components with `@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)` so the migration job can start a non-web Spring context, connect to the database, run Flyway, and exit cleanly.

If a migration job fails with `No ServletContext set`, deploy a backend image that contains this conditional web-configuration fix, then execute the job again.
