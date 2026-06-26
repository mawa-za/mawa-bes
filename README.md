# Mawa BES - Google Secret Manager implementation

These files were generated against the uploaded `mawa-bes.zip` project on branch `phase-1`.

## Changed / added files

```text
pom.xml
src/main/java/za/co/mawa/bes/configuration/gcp/GcpSecretManagerEnvironmentPostProcessor.java
src/main/resources/META-INF/spring.factories
src/main/resources/META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor
src/main/resources/application.properties
docs/GOOGLE_SECRET_MANAGER_SETUP.md
```

## Build note

A compile was attempted with `./mvnw -DskipTests compile`, but the Maven wrapper could not download Maven because this environment could not resolve `repo.maven.apache.org`.

## Cloud Run example

```bash
GCP_SECRET_ENABLED=true
GCP_PROJECT_ID=mawa-prod
GCP_SECRET_MAPPINGS=spring.datasource.password=mawa-db-password,hibernate.connection.password=mawa-db-password,flyway.password=mawa-db-password,jwt.secret=mawa-jwt-secret,mawa.admin.api.password=mawa-admin-api-password
```
