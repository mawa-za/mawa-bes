# Runtime Spring profiles

Backend images are environment-neutral at build time. The active environment is selected at runtime.

Set both variables on every Cloud Run service:

```text
SPRING_PROFILES_ACTIVE=dev|alpha|beta|prep|prod
MAWA_ENVIRONMENT=dev|alpha|beta|prep|prod
```

The two values must describe the same deployed environment. The generic `Dockerfile` defaults to `dev` so an unconfigured development deployment cannot silently start with production configuration. Use `Dockerfile-prod` for a production-default image, although Cloud Run should still explicitly set both variables.
