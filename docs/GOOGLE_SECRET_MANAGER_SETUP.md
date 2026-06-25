# Google Secret Manager setup for Mawa BES

This project now supports loading selected Spring Boot properties from Google Secret Manager before the application context starts.

## What was added

- `GcpSecretManagerEnvironmentPostProcessor`
- Google Cloud Secret Manager Maven dependency
- Spring Boot bootstrap registration under `META-INF`

The loader runs before datasource, JWT and mail properties are bound, so it can override values from `application-*.properties`.

## Required Cloud Run environment variables

```bash
GCP_SECRET_ENABLED=true
GCP_PROJECT_ID=mawa-prod
GCP_SECRET_MAPPINGS=spring.datasource.password=mawa-db-password,hibernate.connection.password=mawa-db-password,jwt.secret=mawa-jwt-secret
```

Mapping format:

```text
spring.property.name=secret-name[:version]
```

Version is optional and defaults to `latest`.

Examples:

```bash
GCP_SECRET_MAPPINGS=jwt.secret=mawa-jwt-secret
GCP_SECRET_MAPPINGS=jwt.secret=mawa-jwt-secret:3
GCP_SECRET_MAPPINGS=spring.mail.password=mawa-mail-password,mawa.admin.api.password=mawa-admin-api-password
```

You may also use full resource names:

```bash
GCP_SECRET_MAPPINGS=jwt.secret=projects/mawa-prod/secrets/mawa-jwt-secret/versions/latest
```

## Recommended mappings for current phase-1 properties

The current environment files contain both Hibernate and Spring datasource password properties. Map both to the same DB password secret so every existing class receives the overridden value:

```bash
GCP_SECRET_MAPPINGS=spring.datasource.password=mawa-db-password,hibernate.connection.password=mawa-db-password,flyway.password=mawa-db-password,jwt.secret=mawa-jwt-secret,mawa.admin.api.password=mawa-admin-api-password
```

## IAM required

The Cloud Run service account must have access to read secrets:

```bash
gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:CLOUD_RUN_SERVICE_ACCOUNT" \
  --role="roles/secretmanager.secretAccessor"
```

For tighter security, grant `roles/secretmanager.secretAccessor` on individual secrets instead of the whole project.

## Local development

Keep this disabled locally unless you have Application Default Credentials configured:

```properties
gcp.secret-manager.enabled=false
```

To test locally:

```bash
gcloud auth application-default login
export GCP_SECRET_ENABLED=true
export GCP_PROJECT_ID=mawa-dev
export GCP_SECRET_MAPPINGS="jwt.secret=mawa-dev-jwt-secret"
./mvnw spring-boot:run -Pdev
```

## Notes

- Do not store new secret values in `application-*.properties`.
- Secret values are not logged. Only the Spring property name and secret reference are logged.
- The secret property source is added first, so it overrides existing property files and environment-derived placeholders.
