# Mawa BES

## Secret management

Sensitive configuration must be supplied at runtime through Google Secret Manager or environment variables. Do not commit real database passwords, JWT secrets, mail passwords, admin API passwords, API keys or tokens.

See:

```text
docs/GOOGLE_SECRET_MANAGER_SETUP.md
```

Minimum Cloud Run settings:

```bash
GCP_SECRET_ENABLED=true
GCP_PROJECT_ID=mawa-162022
GCP_SECRET_MAPPINGS=jwt.secret=mawa-dev-jwt-secret,hibernate.connection.url=mawa-dev-db-url,hibernate.connection.username=mawa-dev-db-username,hibernate.connection.password=mawa-dev-db-password,spring.datasource.url=mawa-dev-db-url,spring.datasource.username=mawa-dev-db-username,spring.datasource.password=mawa-dev-db-password,spring.mail.password=mawa-dev-mail-password,mawa.internal.service-token=mawa-dev-internal-service-token,mawa.admin.api.password=mawa-dev-admin-api-password
```

## Tenant integration secret naming

Xero, FNB and other tenant-specific integration secrets use immutable generated names:

```text
mawa-{environment}-{tenant-host-normalised}-{integration}-{secret-purpose}
```

The backend creates a missing GCP Secret Manager secret when a credential value is submitted and stores only the generated secret reference in tenant configuration.


## Tenant discovery integration

`mawa-bes` discovers tenants from `mawa-admin-bes` through the service-to-service endpoint `GET /internal/erp/tenants` using `X-Mawa-Internal-Token`. Configure the same `mawa.internal.service-token` secret in both services. The old `mawa.admin.api.username/password` login is retained only as a temporary fallback when the internal token is absent.

Tenant metadata is cached for 60 seconds by default (`mawa.admin.api.tenant-cache-ttl-ms`) and stale/local data is used during a temporary admin-service outage.
