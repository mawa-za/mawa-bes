# Admin console internal integration

`mawa-bes` exposes internal endpoints for `mawa-admin-bes`. These endpoints are not for frontend use and require the `X-Mawa-Internal-Token` header.

Internal endpoints:

`mawa-admin-bes` also exposes the reverse service-to-service contract used by `mawa-bes`:

```text
GET  /internal/erp/tenants
GET  /internal/erp/tenants/{tenant}
GET  /internal/erp/tenants/{tenant}/properties
POST /internal/erp/tenants/{tenant}/properties
```

These calls use the same `X-Mawa-Internal-Token` header and remove the former dependency on the mutable Admin Console `admin` password.


```text
POST /internal/admin/handoff
POST /internal/admin/tenant/{tenant}/refresh-config  # pulls tenant properties from mawa-admin-bes into local mawa-bes tenant_property
POST /internal/admin/tenant/{tenant}/modules/sync     # same refresh path, used after module property changes
```

Public handoff exchange endpoint used by `mawa_erp` after admin redirect:

```text
POST /v2/admin-handoff/exchange
```

The handoff token is short-lived, signed by mawa-bes, tenant scoped, safe across Cloud Run instances, and exchanged for normal ERP JWT/refresh tokens.

Required Cloud Run variable on both `mawa-admin-bes` and `mawa-bes`:

```text
MAWA_INTERNAL_SERVICE_TOKEN=<same-random-secret-value>
```

Recommended Secret Manager secret name:

```text
mawa-<env>-internal-service-token
```

Add it to `GCP_SECRET_MAPPINGS` for both services, for example:

```text
mawa.internal.service-token=mawa-beta-internal-service-token
```

The sync endpoints intentionally pull the latest tenant properties from `mawa-admin-bes` instead of trusting the admin frontend. This keeps platform configuration owned by the admin backend.
