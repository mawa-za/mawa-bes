# MAWA backend: remove MawaPay authentication bypass and lengthen normal JWTs

## Included changes

- Removes emergency Bearer/no-token/expired-access/expired-refresh authentication paths.
- Restores strict expiry, signature, user, tenant and token-type checks.
- Restores HTTP 401 on rejected `/v2/refresh-token` calls (no emergency 503).
- Preserves normal device-sync identity and platform-session authentication.
- Production access JWT default: **30 days** (2,592,000,000 ms).
- Production refresh JWT default: **365 days** (31,536,000,000 ms).
- Versions are **not** bumped; other Spring profiles are unchanged.

**Scope:** JWT settings apply to newly issued tokens for **all users of the production
mawa-bes backend**, not only MawaPay. Previously issued JWTs retain their original
embedded expiration; existing devices/users must authenticate again to receive
the new validity periods. No old token is extended retroactively.

## Apply on Windows PowerShell

1. Work on your intended backend branch in a clean `mawa-bes` Git checkout.
2. Extract this ZIP **into the root of `mawa-bes`**. Its `src/` files must overlay
   the matching existing source files.
3. Execute the included removal script from the repository root:

```powershell
.\APPLY-MAWAPAY-AUTH-ROLLBACK.ps1
```

   If PowerShell execution policy blocks this project-local script, run it in
   a PowerShell process with a suitable approved policy. The script removes
   only the precisely listed bypass policy and test files; Git can recover them.

4. Inspect changes and run targeted tests:

```powershell
git status --short
.\mvnw.cmd "-Dtest=JwtTokenUtilExpiredAccessTest,JwtTokenUtilRefreshClaimsTest,JwtTokenUtilLongValidityTest,JwtRequestFilterDeviceSyncPathTest" test
```

5. Run the normal build/release pipeline to deploy the updated backend image.

The production properties now accept explicit environment overrides:

```properties
jwt.expirationDateInMs=${JWT_ACCESS_EXPIRATION_MS:2592000000}
jwt.refreshExpirationDateInMs=${JWT_REFRESH_EXPIRATION_MS:31536000000}
```

## Production Cloud Run environment

**After the updated backend code is deployed**, apply the production JWT
lifetimes and force all emergency flags off. Use **Google Cloud Shell Bash**:

```bash
gcloud run services update mawa-bes-v2-prod \
  --project mawa-162022 \
  --region us-central1 \
  --update-env-vars 'JWT_ACCESS_EXPIRATION_MS=2592000000,JWT_REFRESH_EXPIRATION_MS=31536000000,MAWA_PAY_EMERGENCY_ACCESS_GRACE_ENABLED=false,MAWA_PAY_EMERGENCY_REFRESH_GRACE_ENABLED=false,MAWA_PAY_EMERGENCY_BEARER_ENABLED=false,MAWA_PAY_EMERGENCY_MASTERDATA_NOAUTH_ENABLED=false'
```

This updates environment variables without erasing unrelated Cloud Run settings.
Check the resulting revision and verify a successful login and refresh for a
Phanka Society device. A token still stored on the device does not inherit the
new expiry; reauthenticate the affected devices as planned.

**Security implication:** Access JWTs may remain valid for 30 days if stolen,
and refresh JWTs for 365 days. The existing password-change invalidation check
still applies, but the backend does not maintain a universal instant JWT
revocation list. Plan to restore shorter lifetimes after the device incident.

## Verification status

Archive integrity and source references can be checked here. Maven tests must
be run on your build computer: the current environment lacks a working Maven
distribution and cannot reach Maven Central.
