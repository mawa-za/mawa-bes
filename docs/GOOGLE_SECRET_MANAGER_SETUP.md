# Google Secret Manager setup for Mawa BES

Mawa BES loads sensitive Spring Boot properties from Google Secret Manager during application bootstrap.

The loader runs as a Spring `EnvironmentPostProcessor`, before datasource, JWT and mail configuration are bound. This means Cloud Run can provide a mapping of Spring property names to Google Secret Manager secret names, and the code will inject the secret values into the Spring environment before normal beans start.

## Runtime environment variables

Set these on the Cloud Run service:

```bash
GCP_SECRET_ENABLED=true
GCP_PROJECT_ID=mawa-162022
GCP_SECRET_MAPPINGS=jwt.secret=mawa-dev-jwt-secret,mawa.encryption.secret=mawa-dev-encryption-secret,hibernate.connection.url=mawa-dev-db-url,hibernate.connection.username=mawa-dev-db-username,hibernate.connection.password=mawa-dev-db-password,spring.datasource.url=mawa-dev-db-url,spring.datasource.username=mawa-dev-db-username,spring.datasource.password=mawa-dev-db-password,flyway.url=mawa-dev-db-url,flyway.user=mawa-dev-db-username,flyway.password=mawa-dev-db-password,spring.mail.password=mawa-mail-password,mawa.admin.api.password=mawa-admin-api-password
```

Mapping format:

```text
spring.property.name=secret-name[:version]
```

Version is optional and defaults to `latest`.

Examples:

```bash
GCP_SECRET_MAPPINGS=jwt.secret=mawa-dev-jwt-secret
GCP_SECRET_MAPPINGS=jwt.secret=mawa-dev-jwt-secret:3
GCP_SECRET_MAPPINGS=jwt.secret=projects/mawa-162022/secrets/mawa-dev-jwt-secret/versions/latest
```

## Recommended secret names per environment

Create separate secrets per environment so dev, beta, alpha, prep and prod can rotate independently.

```text
mawa-dev-db-url
mawa-dev-db-username
mawa-dev-db-password
mawa-dev-jwt-secret
mawa-dev-encryption-secret
mawa-dev-mail-password
mawa-dev-admin-api-password

mawa-prod-db-url
mawa-prod-db-username
mawa-prod-db-password
mawa-prod-jwt-secret
mawa-prod-encryption-secret
mawa-prod-mail-password
mawa-prod-admin-api-password
```

Use the environment-specific names in the Cloud Run `GCP_SECRET_MAPPINGS` variable for that service.

## Creating secrets

Example:

```bash
printf '%s' 'jdbc:mysql://HOST/mawa' | gcloud secrets create mawa-dev-db-url --data-file=- --replication-policy=automatic
printf '%s' 'root' | gcloud secrets create mawa-dev-db-username --data-file=- --replication-policy=automatic
printf '%s' 'CHANGE_ME' | gcloud secrets create mawa-dev-db-password --data-file=- --replication-policy=automatic
printf '%s' 'CHANGE_ME_LONG_RANDOM_JWT_SECRET' | gcloud secrets create mawa-dev-jwt-secret --data-file=- --replication-policy=automatic
printf '%s' 'OLD_PRE_ROTATION_ENCRYPTION_SECRET' | gcloud secrets create mawa-dev-encryption-secret --data-file=- --replication-policy=automatic
```

When rotating a value, add a new version instead of changing source code:

```bash
printf '%s' 'NEW_VALUE' | gcloud secrets versions add mawa-dev-db-password --data-file=-
```

## IAM required

The Cloud Run runtime service account must be allowed to read the mapped secrets:

```bash
gcloud secrets add-iam-policy-binding mawa-dev-db-password \
  --member="serviceAccount:CLOUD_RUN_SERVICE_ACCOUNT" \
  --role="roles/secretmanager.secretAccessor"
```

Grant access per secret where possible, not project-wide.

## Cloud KMS / CMEK note

Google Secret Manager encrypts secrets at rest. If MAWA requires customer-managed encryption keys, configure each Secret Manager secret with a Cloud KMS customer-managed key at the GCP resource level. No code change is required for this app; the app still reads the secret through Secret Manager.

## Local development

Keep Secret Manager disabled locally unless the developer has Application Default Credentials configured:

```properties
gcp.secret-manager.enabled=false
```

Local run with environment variables:

```bash
export DB_URL='jdbc:mysql://localhost:3306/mawa'
export DB_USERNAME='root'
export DB_PASSWORD='local-password'
export JWT_SECRET='local-long-random-secret'
export ENCRYPTION_SECRET='old-or-local-encryption-secret'
export MAIL_PASSWORD='local-mail-password'
export MAWA_ADMIN_API_PASSWORD='local-admin-password'
./mvnw spring-boot:run -Pdev
```

Local run through Secret Manager:

```bash
gcloud auth application-default login
export GCP_SECRET_ENABLED=true
export GCP_PROJECT_ID=mawa-162022
export GCP_SECRET_MAPPINGS='jwt.secret=mawa-dev-jwt-secret,mawa.encryption.secret=mawa-dev-encryption-secret,hibernate.connection.password=mawa-dev-db-password,spring.datasource.password=mawa-dev-db-password,flyway.password=mawa-dev-db-password'
./mvnw spring-boot:run -Pdev
```

## JWT secret versus encryption secret

Do not rotate `jwt.secret` and expect existing encrypted passwords to continue working. Older MAWA data used the JWT secret as the AES encryption key for user passwords and tenant database passwords.

Going forward the backend separates these values:

```text
jwt.secret              = token signing key; can be rotated for JWTs
mawa.encryption.secret  = AES key used for existing encrypted passwords/database passwords
```

When moving to Secret Manager, create `mawa-dev-encryption-secret` using the old pre-rotation `jwt.secret` value. After that, `mawa-dev-jwt-secret` can be a new random token-signing secret without breaking login.

If the old encryption secret is lost, existing encrypted user passwords cannot be decrypted; affected users must have their passwords reset using a trusted database/admin process.

## Rules

- Do not commit database passwords, JWT secrets, mail passwords, keystore passwords, API keys, refresh tokens or access tokens.
- Do not commit Android signing keystores or `key.properties`.
- Secret values must not be logged.
- The source repository should only contain placeholders and secret names, never secret values.

## Tenant integration secrets

Runtime integration credentials for Xero and FNB should be stored per tenant in Google Secret Manager. See `docs/TENANT_INTEGRATION_SECRETS.md` for the required tenant property and setting names.
