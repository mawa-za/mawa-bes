# Long-term Cloud Run migration setup

Use the standalone `mawa-flyway-runner` image for all database migrations.

Deployment order:

```text
1. Build and push mawa-flyway-runner.
2. Execute the environment-specific Cloud Run migration job.
3. Require a successful job exit.
4. Deploy or promote mawa-bes.
```

`mawa-bes` remains focused on API traffic and contains no embedded Flyway execution. Flyway environment variables and database migration job arguments belong to `mawa-flyway-runner`, not the API service.
