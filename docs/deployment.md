# Deployment guide

## Production route: Render + TiDB Cloud Starter

This project is ready for a no-cost proof-of-concept deployment using a Render free web service and a TiDB Cloud Starter instance. TiDB is MySQL-compatible, so no application code or database migration changes are required.

1. Create a **TiDB Cloud Starter** instance (not Essential/Dedicated). In its **Connect** dialog, select the public JDBC connection option and copy the generated connection details. TiDB requires TLS, so retain the supplied `sslMode=VERIFY_IDENTITY` and TLS parameters in the JDBC URL.
2. On TiDB's Networking page, add an IP-access rule needed for the Render service. Render free services do not provide a stable outbound IP; use the narrowest supported rule. A broad rule is acceptable only temporarily for this demo because TLS still encrypts traffic.
3. In the Render API service's Environment page, add these secrets:
   - `DB_URL`: the exact TiDB JDBC URL, excluding credentials if they are represented separately.
   - `DB_USERNAME` and `DB_PASSWORD`: the TiDB credentials from Connect.
   - `JWT_SECRET`: a unique random string of at least 32 characters.
   - `EXTERNAL_API_KEY`: a unique random API key used by external integrations.
   - `CORS_ALLOWED_ORIGINS`: the final frontend URL, such as `https://shiphappens-ui.onrender.com`.
   - `DB_MAX_LIFETIME=300000`: keeps pooled connections below TiDB Cloud Starter's recommended maximum lifetime.
4. Deploy from `main` using Dockerfile path `backend/Dockerfile`, then wait for `/actuator/health` to report `UP`. Flyway applies the schema automatically on the first successful start.
5. Deploy `frontend/` as a Render Static Site and set its API base URL to the deployed API URL before publishing it. Do not put database credentials or API keys in the frontend.

TiDB Cloud Starter currently has a free allowance and quotas; check the provider dashboard before launch. It is suitable for the demonstration release, not an uptime or capacity guarantee.

Rollback: redeploy the last compatible application image, verify health/API/database compatibility, and never edit an applied Flyway migration. Do not claim zero-downtime rollback.
