# Funeral tenant integration

## Purpose

A funeral tenant can resolve memberships and create claims in either its own tenant schema, a configured external membership tenant, or both.

## Configuration

Open **System Configuration > Funeral Tenant Integration** in the funeral tenant and maintain:

- **Source mode**: `LOCAL_ONLY`, `EXTERNAL_ONLY`, or `LOCAL_AND_EXTERNAL`.
- **Membership and claims tenant**: the external tenant that owns the memberships and claims.
- **Local invoicing partner**: a partner in the funeral tenant representing the external tenant or burial society for invoice allocation.
- **Membership lookup**, **external claim creation**, **claim status synchronisation**, and **active** switches.

The configuration is stored in the funeral tenant's dedicated `funeral_tenant_integration_config` table. No generic-table configuration is used.

## Runtime behaviour

1. Membership search checks the local tenant, external tenant, or both according to the source mode.
2. A local membership creates its claim in the local `membership_claim` table.
3. An external membership creates its claim in the configured external tenant's `membership_claim` table.
4. The funeral tenant stores a `funeral_service_claim` link containing the source tenant, source membership, source claim ID, physical claim storage scope, and local invoicing-partner mapping.
5. External claims are created as drafts and are reviewed, submitted, approved, rejected, cancelled, or paid from the source membership tenant using its normal claim and approval workflow.
6. Claim status and approved amounts are read live from the tenant that physically owns the claim. Existing legacy external-cover claims that were stored locally remain local after migration.
7. Funeral invoices remain in the funeral tenant and use the configured local partner for the external tenant's approved contribution.

## Database access

The application datasource user must be able to read and write the tenant schemas configured for integration. In the standard MAWA deployment, the same application datasource identity is used for all tenant schemas. Do not share end-user credentials, JWT secrets, or tenant login sessions.

## Deployment order

1. Deploy and run Flyway migration `V202607150004__funeral_tenant_integration.sql` for all tenant schemas.
2. Deploy `mawa-bes`.
3. Deploy `mawa_erp`.
4. Configure the integration from the funeral tenant.

## Endpoints

- `GET /v2/funeral/tenant-integration`
- `PUT /v2/funeral/tenant-integration`
- `GET /v2/funeral/tenant-integration/available-tenants`
- `GET /v2/funeral/check-membership/{identityNumber}`
- `POST /v2/funeral/service-request/{id}/initiate-claims`
- `GET /v2/funeral/service-request/{id}/claims`
