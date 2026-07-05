# Session Refresh, Funeral Claim Documents and Queue Scheduling

## Session refresh

The ERP sends refresh tokens to `/v2/refresh-token` using all supported forms:

- `Authorization: Bearer <refresh-token>`
- `Refresh-Token: <refresh-token>`
- JSON body `refreshToken` and `refresh_token`

The backend now accepts all of these forms. This fixes the previous mismatch where the ERP sent the refresh token in the body, while the backend only read the Authorization header.

## Funeral arrangement claim submission

Funeral Arrangement claim initiation now creates membership claims in `DRAFT` status. Users can attach claim documentation to each claim before submitting it for approval.

Flow:

1. Create funeral service request.
2. Initiate claims from selected covers.
3. Claims are created as `DRAFT`.
4. User attaches documents against the claim id.
5. User submits each claim for approval.
6. The existing `ApprovalType.CLAIM` workflow submits the membership claim and generates the claim form.
7. Approval completion marks the claim as approved and continues the existing payout handling.

## Message queue scheduling

Message Queue Administration now supports tenant settings under group `MESSAGE-QUEUE`:

- `ENABLED`
- `INTERVAL-SECONDS`
- `BATCH-SIZE`

Endpoints:

- `GET /v2/message-queue/schedule`
- `PUT /v2/message-queue/schedule`
- `POST /v2/message-queue/schedule/start`
- `POST /v2/message-queue/schedule/stop`
