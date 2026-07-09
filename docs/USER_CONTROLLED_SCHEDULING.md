# User-controlled scheduling

Hardcoded cron/fixed job schedules have been replaced with dispatcher-driven jobs whose effective intervals and enabled status are stored in tenant settings.

## ERP tenant schedules

### Message queue

Configured from ERP frontend: **System Configuration → Message Queue Processing**.

Endpoints:

```text
GET  /v2/message-queue/schedule
PUT  /v2/message-queue/schedule
POST /v2/message-queue/process-now
```

Settings group: `MESSAGE-QUEUE`

```text
ENABLED
INTERVAL-SECONDS
BATCH-SIZE
RETRY-DELAY-SECONDS
```

### Legacy membership migration

Configured from ERP frontend: **System Configuration → Scheduled Jobs**.

Endpoints:

```text
GET  /v2/scheduler/jobs
PUT  /v2/scheduler/jobs/{jobCode}
POST /v2/scheduler/jobs/{jobCode}/run-now
```

Current job code:

```text
LEGACY_MEMBERSHIP_MIGRATION
```

Settings group: `LEGACY-MIGRATION`

```text
ENABLED
INTERVAL-MINUTES
LAST-RUN-AT
```

Keep this disabled after legacy migration has completed.

## Dispatcher

The lightweight dispatcher interval is controlled by environment/application property:

```text
mawa.scheduler.dispatcher-delay-ms=30000
```

This does not define business job timing. It only checks whether user-configured jobs are due.
