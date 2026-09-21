# ShipHappens implementation plan

Last updated: 2026-09-22

## Product boundary

ShipHappens is a Java 17 / Spring Boot modular monolith with a plain HTML/CSS/JavaScript frontend. It manages the complete shipment workflow, integrates external customer orders with API-key authentication and idempotency, publishes outbound webhooks, and provides operational reporting. MySQL is the production and local container database; Flyway is the sole schema-change mechanism.

## Operating rules

- Keep controllers thin. Validation occurs at the API boundary; business decisions and transactions belong in services; repositories own persistence queries.
- Use DTOs for all public API responses before adding new endpoints. Do not expose JPA entities; some early endpoints currently do and must be refactored as Phase 2 work.
- Never change an already-applied Flyway migration. Add a new versioned migration for every schema change.
- No secrets in source, documentation, logs, test snapshots, or committed `.env` files.
- Preserve `X-Request-ID` from the caller, generate one when absent, return it in responses, and include it in logs.
- Do not report a test, Docker run, or deployment as successful unless it was actually executed in the current environment.

## Phased roadmap

| Phase | Outcome | Main deliverables | Definition of done |
|---|---|---|---|
| 0 | Verified foundation | Maven, Docker, MySQL, Flyway, health endpoint | `mvn test package`; MySQL startup; migrations; `/actuator/health` verified |
| 1 | Core domain integrity | Customers, addresses, orders, packages, shipments, tracking, drivers, vehicles, warehouses | All required constraints and state transitions covered by tests |
| 2 | Complete operations API | DTO/mappers, CRUD, pagination/filtering, cancellation, assignment/delivery actions | API contract documented in Swagger and Postman |
| 3 | Security | App users, BCrypt, JWT login/refresh, roles and ownership authorization | Unauthorized/forbidden/ownership tests pass |
| 4 | Integration reliability | API-key integration endpoint, mapping service, idempotency, integration-request logging | Valid/invalid/duplicate workflows verified |
| 5 | Webhooks | Subscriptions, HMAC signatures, asynchronous delivery, exponential retries, delivery logs, mock receiver | Success and retry-exhaustion tests pass |
| 6 | Reporting and UI | SQL aggregations, dashboard API, frontend views, tracking UI | Browser UI consumes only API responses and shows API errors |
| 7 | Quality and delivery | Unit/controller/integration tests, Postman, docs, CI, Docker image | Clean-checkout instructions are verified |
| 8 | Deployment | Managed database, HTTPS backend/frontend, restricted CORS, live URLs | Production checklist completed with real evidence |

## Recommended implementation order

1. Finish Phase 0 first. Validate the existing migration against the live MySQL container and replace direct JPA-entity API responses with response DTOs.
2. Finish every core operation end to end: create customer → create order with packages → shipment → assignment → allowed status changes → delivery completion/resource release.
3. Add security before exposing any operational endpoint publicly.
4. Build integration logging and webhooks only after internal shipment events are reliable.
5. Add reporting/UI after the APIs return stable DTO contracts.
6. Add test coverage alongside each phase; do not defer all tests to the end.
7. Deploy only after CI, secret review, CORS, migrations, and verification commands are complete.

## API target inventory

Required base path: `/api/v1`.

- Customers: create, list, get, update, status update.
- Orders: create, list, get, update, cancel; nested package create/list.
- Shipments: create/list/get, status update, chronological tracking; filters and pagination.
- Warehouses, drivers, vehicles: CRUD; warehouse capacity enforcement.
- Deliveries: assign/list/get, status update/fail; release driver and vehicle when terminal.
- Integrations: `POST /integrations/orders` with `X-API-KEY`, request logging, idempotent behavior.
- Webhooks: subscription management, local mock receiver, retry logs.
- Reports: status summary, failure summary, warehouse utilization, delivery performance.
- Auth: login and JWT workflow before production access.

## Verification matrix

| Area | Command / scenario |
|---|---|
| Build | `mvn test package` from `backend` |
| Database | `docker compose up -d mysql`, then inspect Flyway startup logs |
| Backend | `mvn spring-boot:run`, `GET /actuator/health` |
| API | Swagger `http://localhost:8080/swagger-ui/index.html` |
| Frontend | Serve `frontend`; dashboard loads metrics and shipment list |
| Workflow | Customer → order/packages → assign → pickup → transit → warehouse → out for delivery → delivered |
| Integration | valid API key, invalid key, invalid payload, duplicate external reference |
| Webhook | 2xx delivery, non-2xx retry, final failed state |
| Docker | `docker compose up --build`, backend health response |
| CI | GitHub Actions runs build and tests on push/PR |

## Open architecture decisions

- Use JWT with a signed symmetric key from `JWT_SECRET`; do not add OAuth until the basic auth model is stable.
- Use a database-backed webhook-delivery log and a scheduled worker for retries; do not sleep inside HTTP request threads.
- Use an application user table that maps customer/driver ownership explicitly, instead of inferring identity from request parameters.
- Use `Pageable` and repository projections for reports/list endpoints to prevent N+1 and oversized responses.
- Keep frontend deployable independently by configuring its API base URL at build/runtime; avoid hard-coded production URLs.
