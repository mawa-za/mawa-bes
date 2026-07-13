# MAWA Pay large membership sync

Endpoint: `GET /v2/pay-app/members?page=0&size=500`

The endpoint uses SQL-level paging and returns only fields needed by MAWA Pay. Page size is
bounded to 2000 to protect Cloud Run and database resources. MAWA Pay persists each page before
requesting the next one, so 10,000+ memberships no longer depend on one large `/v2/partner`
response.
