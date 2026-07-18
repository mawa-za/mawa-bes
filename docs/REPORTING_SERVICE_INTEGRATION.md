# Reporting service integration

`mawa-bes` remains the transactional API and does not execute reporting queries.
Its only reporting change is the `reports` entry in `WorkcenterService`, because
that service owns the ERP workcentre catalogue used by Role Maintenance and the
home screen.

Report data is served by the separately deployed `mawa-reporting-bes` service.
