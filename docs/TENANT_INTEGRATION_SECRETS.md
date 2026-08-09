# Tenant integration secrets in Google Secret Manager

Mawa BES supports tenant-specific integration secrets for Xero and FNB through Google Secret Manager.

Tenant settings should store **secret names/references**, not passwords, client secrets, refresh tokens or API keys.

## Required Cloud Run configuration

The Cloud Run service still needs the GCP project id so the application can resolve tenant secret references:

```bash
GCP_PROJECT_ID=mawa-162022
```

The Cloud Run service account needs `Secret Manager Secret Accessor` on all integration secrets that the service must read.

For Xero OAuth refresh token updates, the service account also needs permission to add secret versions on the refresh-token secret. Use `Secret Manager Secret Version Adder` where available, or a custom role with `secretmanager.versions.add`.

## Secret reference formats

Any tenant property or setting can reference a secret using one of these formats:

```text
gcp-secret://mawa-dev-localhost-xero-secret-key
sm://mawa-dev-localhost-xero-secret-key
projects/mawa-162022/secrets/mawa-dev-localhost-xero-secret-key/versions/latest
mawa-dev-localhost-xero-secret-key:3
```

When using a `*-SECRET` companion property/setting, a plain secret name is also supported:

```text
mawa-dev-localhost-xero-secret-key
```

## Xero tenant properties

For each tenant, store the references below as tenant properties:

```text
XERO-CLIENT-ID-SECRET=mawa-dev-<tenant>-xero-client-id
XERO-SECRET-KEY-SECRET=mawa-dev-<tenant>-xero-secret-key
XERO-REFRESH-TOKEN-SECRET=mawa-dev-<tenant>-xero-refresh-token
XERO-TENANT-ID-SECRET=mawa-dev-<tenant>-xero-tenant-id
```

Optional:

```text
XERO-ACCESS-TOKEN-SECRET=mawa-dev-<tenant>-xero-access-token
XERO-REDIRECT-URL-SECRET=mawa-dev-<tenant>-xero-redirect-url
XERO-BASE-URL-SECRET=mawa-dev-<tenant>-xero-base-url
```

Non-sensitive tenant properties can remain as plain values:

```text
XERO-INVOICE-INTEGRATION-ENABLED=true
XERO-INVOICE-STATUS=DRAFT
XERO-INVOICE-ACCOUNT-CODE=200
XERO-INVOICE-TAX-TYPE=NONE
XERO-LINE-AMOUNT-TYPES=Exclusive
```

If one tenant uses another tenant's Xero setup, keep this property on the consuming tenant:

```text
XERO-MAWA-SERVICE-PROVIDER-LINK=<provider-tenant-code>
```

The provider tenant must have its own Xero `*-SECRET` references configured.

## FNB tenant settings

FNB values are read from tenant-specific settings in the tenant schema. Store secret references as settings in the same setting group.

Recommended settings:

```text
Setting group: FNB-API
CLIENT-ID-SECRET=mawa-dev-<tenant>-fnb-client-id
CLIENT-SECRET-SECRET=mawa-dev-<tenant>-fnb-client-secret
BASE-URL=https://...
POP-RECIPIENT=<email address>
```

Sensitive bank account values can also be stored in Secret Manager:

```text
Setting group: EFT-BANK-ACCOUNT
ACCOUNT-NUMBER-SECRET=mawa-dev-<tenant>-eft-account-number
ACCOUNT-HOLDER=<account holder>
BRANCH-CODE=<branch code>
ACCOUNT-TYPE=CACC
```

## Creating secrets

Example:

```bash
printf '%s' '<actual-xero-client-secret>' | gcloud secrets create mawa-dev-localhost-xero-secret-key \
  --project=mawa-162022 \
  --replication-policy=automatic \
  --data-file=-
```

Add/rotate a value:

```bash
printf '%s' '<new-value>' | gcloud secrets versions add mawa-dev-localhost-xero-secret-key \
  --project=mawa-162022 \
  --data-file=-
```

## Backward compatibility

The application can still read existing plain tenant properties/settings as a fallback so current tenants do not immediately break. New tenants and rotated credentials should use Google Secret Manager references only.
