# FNB, Message Queue and Claim Form Administration

## FNB Integration Administration

The administration endpoint is:

- `GET /v2/integrations/fnb/settings`
- `PUT /v2/integrations/fnb/settings`

The backend stores tenant-specific settings in the existing `settings` table:

- Group `FNB-API`
  - `ENABLED`
  - `BASE-URL`
  - `CLIENT-ID-SECRET`
  - `CLIENT-SECRET-SECRET`
  - `POP-RECIPIENT`
- Group `EFT-BANK-ACCOUNT`
  - `ACCOUNT-NUMBER-SECRET`
  - `ACCOUNT-HOLDER`
  - `BRANCH-CODE`
  - `ACCOUNT-TYPE`
  - `BANK-NAME`

Secret settings must point to Google Secret Manager secret names. The secret values must not be stored in the database.

## Message Queue Processing Administration

The administration endpoint is:

- `GET /v2/message-queue`
- `GET /v2/message-queue/{id}`
- `POST /v2/message-queue/{id}/retry`
- `POST /v2/message-queue/{id}/mark-processed`
- `POST /v2/message-queue/process-now`

The screen can be used to monitor pending, waiting, processed and failed messages. Retry resets the selected row to unprocessed, retry count zero and next attempt now.

## Claim Form Generation

When a membership claim is submitted, a PDF claim form is generated automatically and stored as an attachment:

- object type: `claims`
- object id: `membership_claim.id`
- document type: `CLAIM-FORM`

Manual generation/download endpoints are also available:

- `POST /v2/membership-claim/{id}/claim-form`
- `GET /v2/membership-claim/{id}/claim-form`

For funeral service requests, claims inserted in `SUBMITTED` status also trigger claim form generation.
