# Membership migration

The membership migration endpoint migrates legacy membership transactions into the v2 membership tables and now migrates dependents directly from `transaction_partner`.

## Execution

Legacy membership migration is not exposed as an HTTP endpoint. In particular,
`POST /v2/membership/migrate` and its former `GET` compatibility route are no
longer available. Migration execution is restricted to the configured internal
scheduler while it remains enabled.

## What is migrated

For each tenant:

1. Old membership products are migrated into `membership_plan`.
2. Old `MEMBERSHIP` transactions are migrated into `membership`.
3. Legacy dependent partners are migrated from `transaction_partner` into `membership_dependent` where `partner_function` is `DEPENDENT` or `DEPENDANT`.

The migration is idempotent. Running it more than once will not duplicate dependents because `membership_dependent` has a unique key on `membership_id` and `dependent_partner_id`, and the service checks for existing rows before insert.

## Important behaviour

- The migration no longer uses `MembershipService.createMembership`, because that allocates a new membership number and can replace the old number.
- Memberships are saved directly with the old transaction number and `old_id`.
- Dependents are read directly from `transaction_partner` instead of `DependentService.get(...)`, which previously swallowed partner lookup errors.
- One failed membership no longer stops the full tenant migration.
- The current legacy plan is selected deterministically from the newest
  `transaction_item` (`valid_from DESC, item DESC`), rather than from the
  non-deterministic item exposed by the legacy transaction view.
- Re-running the migration does not overwrite the plan or premium of an
  existing v2 membership. This protects approved plan and premium changes.
- `V202608290001__repair_migrated_membership_plan_selection.sql` repairs
  affected migrated memberships, excludes memberships with v2 plan-change
  requests, and writes each correction to `membership_change_audit`.
- The response includes counts and warnings per tenant.

## Response example

```json
{
  "tenantsProcessed": 1,
  "oldMembershipsFound": 100,
  "membershipsCreated": 10,
  "membershipsUpdated": 90,
  "dependentsCreated": 240,
  "dependentsAlreadyExisting": 10,
  "dependentsSkipped": 0,
  "tenants": [
    {
      "tenantId": "dev.app.mawa.co.za",
      "oldMembershipsFound": 100,
      "membershipsCreated": 10,
      "membershipsUpdated": 90,
      "dependentsCreated": 240,
      "warnings": [],
      "errors": []
    }
  ]
}
```

## SQL backfill

The Flyway script `V202607050002__backfill_membership_dependents_from_transaction_partner.sql` performs a database-level backfill for tenants where the v2 membership rows already exist but dependents are missing.
