# Number Range Configuration

## Scope

Tenant administrators manage number ranges from **System Configuration → Number Range Configuration** in MAWA ERP.

The screen separates the two numbering mechanisms that currently exist in the tenant schema:

- **Operational Sequences** (`number_sequence`) for current modules and offline device allocations.
- **Document Ranges** (`number_range`) for the existing stored-procedure transaction generator.

## API

Base path: `/v2/number-range-configuration`

- `GET /sequences`
- `GET /sequences/{id}`
- `POST /sequences`
- `PUT /sequences/{id}`
- `GET /allocations?seqType=&deviceId=`
- `GET /document-ranges`
- `POST /document-ranges`
- `PUT /document-ranges/{id}`
- `GET /audit?sourceType=&rangeKey=`

## Safety rules

- Sequence types and legacy objects are immutable after creation.
- Start numbers are immutable for operational sequences.
- Next/current numbers cannot be decreased.
- End numbers cannot be moved below numbers already issued.
- Inactive or exhausted operational sequences cannot issue numbers.
- Device allocations cannot exceed the configured end number.
- When an allocation request omits `allocationSize`, the configured default block size is used.
- Configuration changes are written to `number_range_configuration_audit`.

## Deployment

Run Flyway migration `V202607210001__number_range_configuration.sql` before deploying the updated `mawa-bes` service.
