# MAWA BES development profile deployment

The `mawa-bes-dev` Cloud Run service must run with the Spring profile `dev`.

The development image is built from `Dockerfile-dev`, which now enforces:

```text
SPRING_PROFILES_ACTIVE=dev
-Dspring.profiles.active=dev
```

Cloud Build / Cloud Run trigger requirements:

1. Use `Dockerfile-dev` as the Dockerfile.
2. Do not set `SPRING_PROFILES_ACTIVE=beta` on the `mawa-bes-dev` service.
3. After deployment, verify the startup log contains:

```text
The following 1 profile is active: "dev"
```

If the service still reports `beta`, the trigger is building a different Dockerfile or overrides the container command.
