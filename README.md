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
GCP_SECRET_MAPPINGS=jwt.secret=mawa-dev-jwt-secret,hibernate.connection.url=mawa-dev-db-url,hibernate.connection.username=mawa-dev-db-username,hibernate.connection.password=mawa-dev-db-password,spring.datasource.url=mawa-dev-db-url,spring.datasource.username=mawa-dev-db-username,spring.datasource.password=mawa-dev-db-password,spring.mail.password=mawa-dev-mail-password,mawa.admin.api.password=mawa-dev-admin-api-password
```

## Tenant integration secret naming

Xero, FNB and other tenant-specific integration secrets use immutable generated names:

```text
mawa-{environment}-{tenant-host-normalised}-{integration}-{secret-purpose}
```

The backend creates a missing GCP Secret Manager secret when a credential value is submitted and stores only the generated secret reference in tenant configuration.
