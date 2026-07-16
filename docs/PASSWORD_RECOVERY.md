# Password recovery

The public password recovery flow uses:

- `POST /v2/forgot-password`
- `POST /v2/reset-password`

Reset tokens are 256-bit opaque random values. Only a SHA-256 hash is stored in the tenant schema. Tokens expire after 30 minutes by default, are single-use, and a new request consumes earlier active tokens for the same user.

Successful password changes set `user.password_changed_at`; access and refresh tokens issued before that timestamp are rejected.

Configuration:

- `MAWA_PASSWORD_RESET_EXPIRATION_MS` (default `1800000`)
- `MAWA_PASSWORD_RESET_EMAIL_RATE_LIMIT` (default `5` per hour)
- `MAWA_PASSWORD_RESET_IP_RATE_LIMIT` (default `20` per hour)
- `MAWA_PASSWORD_RESET_MINIMUM_PASSWORD_LENGTH` (default `8`)

The forgot-password response is deliberately neutral and does not disclose whether an email address exists.
