# Tenant host resolution

Public authentication endpoints resolve a tenant from the first available value:

1. `X-TenantID`
2. `X-Tenant-Id` (compatibility alias)
3. Browser `Origin`
4. Browser `Referer`

The value is normalised before it is compared with the tenant `host` maintained by the admin service. Schemes, ports, paths, query strings, Flutter hash routes, trailing dots and letter case are removed or normalised.

Examples that all resolve to `dev.app.mawa.co.za`:

- `dev.app.mawa.co.za`
- `dev.app.mawa.co.za/#/login`
- `https://dev.app.mawa.co.za/#/login`
- `HTTPS://DEV.APP.MAWA.CO.ZA:443`
