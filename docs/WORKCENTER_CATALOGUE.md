# Central Workcenter Catalogue

`workcenter_catalog` is the authoritative source for workcenter permission and navigation metadata.

It owns the workcenter ID and permission code, route, group, section, labels, descriptions, icon,
display ordering, active state and whether administrators may assign the workcenter to roles.

## Runtime flow

1. Flyway creates and seeds the catalogue in every tenant schema.
2. `WorkcenterService` reads the current tenant's catalogue through the tenant-routed datasource.
3. `GET /v2/workcenters` supplies the role-maintenance catalogue.
4. Role-workcenter responses contain the same metadata for ERP navigation.
5. ERP uses catalogue metadata first. Its compiled route/group registries and tenant-experience JSON
   are compatibility fallbacks for tenants that have not completed rollout.

Role assignments remain in `role_workcenter`. Group rows are presentation metadata and never grant
access. The backend rejects assignments for unknown, inactive or non-assignable catalogue entries.

Approval tiles are an exception to role-workcenter assignment: they are generated from active
workflow approver rules. A user sees the Approvals group only when at least one approval type is
assigned to that user, one of their roles/groups, or their manager scope.

## Adding or changing a workcenter

Change the catalogue with a tenant Flyway migration. Do not add a Java hard-coded catalogue entry,
a Flutter grouping entry or tenant-experience JSON merely to expose a workcenter. The target route
must already exist in the deployed ERP version.

## Deployment order

1. Deploy and run `mawa-flyway-runner`.
2. Deploy `mawa-bes`.
3. Deploy `mawa_erp`.

The migration preserves unknown historic role assignments as active but non-assignable catalogue
records so rollout does not delete access data. Complete their metadata in a later migration before
making them assignable.
