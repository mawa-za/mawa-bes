# Long-term Cloud Run migration setup

Use the standalone `mawa-flyway-runner` image for migrations.

Deployment order:

```text
1. Build/push mawa-flyway-runner.
2. Execute Cloud Run Job: mawa-bes-dev-migration.
3. If the job exits 0, deploy/promote mawa-bes.
4. Keep mawa-bes Flyway startup disabled.
```

`mawa-bes` should remain focused on serving API traffic only.
