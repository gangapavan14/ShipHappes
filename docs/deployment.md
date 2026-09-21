# Deployment guide

No production deployment has been performed. Use a managed MySQL service, configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (32+ characters), `EXTERNAL_API_KEY`, and restricted `CORS_ALLOWED_ORIGINS` in provider secret storage. Run Flyway migrations through application startup, expose only HTTPS, and verify `/actuator/health`, Swagger, login, integration idempotency, and webhook delivery after release.

Rollback: redeploy the last compatible application image, verify health/API/database compatibility, and never edit an applied Flyway migration. Do not claim zero-downtime rollback.
