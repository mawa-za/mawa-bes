# Database migration ownership

Core backend does not execute Flyway and does not package active database migrations.

- `spring.flyway.enabled=false`
- Hibernate schema mutation remains disabled/validation-only according to the service configuration.
- All database changes must be added to and executed by `mawa-flyway-runner`.
- Deploy/run the migration job before deploying this service.
