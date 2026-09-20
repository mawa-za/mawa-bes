# Card terminal endpoint fix

Target branch: `card-payment-fix`

Base: production `v2.0.41-release` / commit `1220801f`

Patch commit: `4856e0dd`

This restores the card-terminal management feature that was present on
`bug-fixes-for-v2` but was not included in the production release.

Endpoints restored:

- `GET /v2/card-terminals?activeOnly=false`
- `GET /v2/card-terminals?activeOnly=true`
- `POST /v2/card-terminals`
- `PUT /v2/card-terminals/{id}`

The patch also restores card-terminal validation on card payments and the
terminal fields used by card cashups.

## Apply

Copy the included files over the matching repository paths, or apply
`card-terminal-endpoints.patch` with:

```bash
git apply card-terminal-endpoints.patch
```

Build and deploy `mawa-bes` after the corresponding `card_terminal` database
migration has been applied to tenant schemas.

## Verification

`git diff --check` passes. A Maven compile could not be executed in the
packaging environment because Maven dependencies were unavailable from the
restricted network.
