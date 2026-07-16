# Tenant admin integration 401/503 fix

## Root cause

`mawa-bes` previously logged into `mawa-admin-bes` as the human/bootstrap `admin` user for every tenant refresh. The integration therefore failed when that password changed, the account policy changed, the admin service was temporarily starting, or the issued token was rejected. Tenant resolution for `/v2/authenticate` and the message queue scheduler both depended on this call.

## Implemented contract

- `mawa-admin-bes` exposes `/internal/erp/**` endpoints protected by `X-Mawa-Internal-Token`.
- `mawa-bes` uses that token for tenant discovery and property reads/writes.
- Tenant discovery is cached for 60 seconds and uses stale/local tenant data during a temporary outage.
- Admin API calls now have connection/read timeouts and useful response-body diagnostics.
- The duplicate `RemoteTenantService` delegates to the hardened implementation.
- Legacy admin username/password authentication remains only as a compatibility fallback when no internal token is configured.

## Required deployment configuration

Create one environment-specific Secret Manager secret, for example `mawa-dev-internal-service-token`, and map the exact same value into both Cloud Run services:

```text
mawa.internal.service-token=mawa-dev-internal-service-token
```

Deploy `mawa-admin-bes` before `mawa-bes`. After both deployments, `MAWA_ADMIN_API_PASSWORD` is no longer required for tenant discovery, although it may be retained during the transition.

## Verification

1. `GET /internal/erp/tenants` without the internal header returns 401.
2. The same request with the correct header returns 200 and a tenant array.
3. ERP login from `https://dev.app.mawa.co.za` resolves its tenant and `/v2/authenticate` is no longer affected by Admin Console user credentials.
4. Cloud Logging no longer shows repeated `TenantAdminService.getAdminToken` failures from `MessageConsumerService.processAllTenants`.
