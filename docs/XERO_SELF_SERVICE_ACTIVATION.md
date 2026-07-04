# Xero self-service activation

MAWA supports tenant-level Xero activation from the Settings screen.

## Runtime flow

1. User logs into MAWA for a tenant.
2. User opens Settings > XERO.
3. User enters Xero Client ID and Client Secret.
4. Backend writes those values to Google Secret Manager.
5. Backend stores only secret names in tenant settings.
6. Backend returns a Xero authentication URL.
7. User opens the URL and authorises the correct Xero organisation.
8. Xero redirects back to `/xero/callback` with the MAWA tenant in OAuth `state`.
9. Backend stores refresh token and Xero tenant ID in Google Secret Manager.
10. Invoice push is enabled for the tenant.

## Settings written by activation

Group: `XERO`

| Attribute | Value |
| --- | --- |
| `CLIENT-ID-SECRET` | GCP secret name for Xero Client ID |
| `SECRET-KEY-SECRET` | GCP secret name for Xero Client Secret |
| `REFRESH-TOKEN-SECRET` | GCP secret name for Xero refresh token |
| `TENANT-ID-SECRET` | GCP secret name for Xero tenant ID |
| `REDIRECT-URL` | Public backend Xero callback URL |
| `INVOICE-INTEGRATION-ENABLED` | `true` or `false` |

## Secret naming

Secrets are generated as:

```text
mawa-{spring-profile}-{tenant-host-normalised}-xero-{purpose}
```

Example for dev tenant `dev.app.mawa.co.za`:

```text
mawa-dev-dev-app-mawa-co-za-xero-client-id
mawa-dev-dev-app-mawa-co-za-xero-secret-key
mawa-dev-dev-app-mawa-co-za-xero-refresh-token
mawa-dev-dev-app-mawa-co-za-xero-tenant-id
```

## Required Cloud Run IAM permissions

The Cloud Run runtime service account needs permissions to create/update/read tenant integration secrets.

Minimum recommended role for self-service activation:

```text
roles/secretmanager.admin
```

If the secret containers are pre-created by an administrator, the service account can instead use narrower permissions:

```text
roles/secretmanager.secretAccessor
roles/secretmanager.secretVersionAdder
```

## Endpoints

```http
POST /v2/integrations/xero/activate
POST /v2/integrations/xero/deactivate
```

Activation payload:

```json
{
  "clientId": "...",
  "clientSecret": "...",
  "redirectUrl": "https://dev.api.app.mawa.co.za",
  "invoiceIntegrationEnabled": true
}
```

The response contains `authenticationUrl`. Open it to authorise the Xero organisation.

## Deactivation

Deactivation sets:

```text
Group: XERO
Attribute: INVOICE-INTEGRATION-ENABLED
Value: false
```

It does not delete secret references or secret values. This allows easy reactivation/reconnect later.


## Tenant key used for generated secret names

Self-service activation now generates host-based secret names by default. The backend uses `TenantContext.getCurrentTenantURL()` when available, otherwise it looks up the tenant record by the current tenant id and uses the tenant `host`.

Example for tenant host `dev.app.mawa.co.za`:

```text
mawa-dev-dev-app-mawa-co-za-xero-client-id
mawa-dev-dev-app-mawa-co-za-xero-secret-key
mawa-dev-dev-app-mawa-co-za-xero-refresh-token
mawa-dev-dev-app-mawa-co-za-xero-access-token
mawa-dev-dev-app-mawa-co-za-xero-tenant-id
```

Existing tenant-id-based secret names are still supported if they are already saved in Group `XERO` settings. A tenant will move to host-based names the next time Xero is activated or reconnected through MAWA Settings.
