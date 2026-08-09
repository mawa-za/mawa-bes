# Operational Configuration, Forms, Pickup and Membership Controls

Implemented on 2026-07-26 against the supplied `mawa-bes(63).zip` source.

## Backend changes

- Enforces shared reference data for `BANK-NAME`, `BANK-ACCOUNT-TYPE`, `PROVINCE`, `SALES-AREA`, `CAUSE-OF-DEATH`, and company-form categories.
- Enforces exactly 10 numeric digits for applicable contact-number inputs.
- Supplier Invoice payment requests:
  - only accept approved suppliers;
  - always use EFT;
  - load approved, currently valid supplier banking details;
  - reject changes to supplier, payment method, or banking details;
  - return clear missing/unapproved banking messages.
- Reusable warehouse, storage-location, and bin configuration APIs.
- Pickup questionnaire fields, injury-photo completion guard, and mandatory storage selection.
- Configurable claim types, burial date, configured cause of death, and plan-benefit-derived claim amount.
- FNB administration reads the debtor account from Payment Account Configuration.
- Dedicated membership policy controls for multiple memberships and optional approval.
- Additional memberships requiring approval are created as `PENDING_APPROVAL` and activated by the `ADDITIONAL_MEMBERSHIP` approval handler.
- Central company forms with version replacement, GCS-backed attachments, active publication, inline preview/download, and protected-system-administrator publishing controls.
- Third-party funeral underwriting APIs remain domain-specific and are consumed by the completed Flutter administration screens.

## New API groups

- `/v2/storage-configuration`
- `/v2/claim-type-configuration`
- `/v2/membership-policy-configuration`
- `/v2/company-forms`

## Deployment

1. Run the matching `mawa-flyway-runner` tenant migration first.
2. Deploy this backend.
3. Deploy the matching `mawa_erp` application.
4. Confirm the attachment GCS bucket and service-account permissions are configured for company forms and pickup injury photos.
5. Maintain bank names, account types, provinces, sales areas, and causes of death through Field Configuration.

## Validation performed

- Java delimiter/source-structure validation.
- Targeted `javac` parsing produced no syntax diagnostics; unresolved framework/Lombok types are expected without Maven dependencies.
- API/migration/frontend integration markers checked.

A full Maven build could not be executed in the offline sandbox because Maven dependencies and the wrapper distribution were not locally available.
