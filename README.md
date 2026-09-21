# ShipHappens 🚚

## Logistics Order & Integration Management Platform

ShipHappens is a modular-monolith logistics platform for creating customer orders, tracking shipments through controlled delivery states, assigning operational resources, and receiving orders from external systems.

## Current build status

The foundation is in place: Spring Boot service structure, MySQL/Flyway schema, Docker Compose definition, request correlation IDs, consistent errors, customer/order/shipment workflows, external-order API-key protection, idempotent external-reference lookup, and a static operational dashboard. The local MySQL container, Flyway startup, health endpoint, dashboard, and seeded shipment retrieval have been verified.

This is an in-progress build. JWT/role authorization, package persistence, integration mapping/logging, webhook delivery/retry, seed data, initial reporting, Postman collection, CI, and baseline documentation are implemented. Complete DTO coverage, ownership authorization, full CRUD/filtering, warehouse workflow, broad automated test coverage, and deployment remain before it meets the full specification.

## Prerequisites

- Java 17
- Maven 3.9+
- Docker Desktop (recommended, for MySQL)

## Start local infrastructure

```powershell
docker compose up -d mysql
cd backend
$env:DB_URL='jdbc:mysql://localhost:3307/shiphappens'
mvn spring-boot:run
```

Serve `frontend` with any static server, such as VS Code Live Server, then open `index.html`. The API is at `http://localhost:8080/api/v1`, Swagger at `http://localhost:8080/swagger-ui/index.html`, and health at `http://localhost:8080/actuator/health`.

Copy `.env.example` to `.env` and replace the integration key before exposing the application anywhere. This repository includes an implementation roadmap and exact successor-agent state in [`docs/implementation-plan.md`](docs/implementation-plan.md) and [`docs/agent-handoff.md`](docs/agent-handoff.md).

## Deployment tomorrow checklist

1. Create a GitHub repository and push this local Git repository.
2. Create a managed **MySQL** database and a Docker web service using [`render.yaml`](render.yaml), or use any provider that supports Docker and environment variables.
3. Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (32+ random characters), `EXTERNAL_API_KEY`, `CORS_ALLOWED_ORIGINS`, and optionally `DEMO_PASSWORD` as host secrets. Never use the local values from `.env.example` in production.
4. Deploy `frontend` separately as a static site (Netlify configuration is included), then set `CORS_ALLOWED_ORIGINS` to its real HTTPS domain and set `window.SHIPHAPPENS_API` before loading `frontend/app.js` if the API domain differs from localhost.
5. Verify `/actuator/health`, Swagger, JWT login, a protected report, external order submission/idempotency, and at least one webhook endpoint over HTTPS.

There is no deployed URL yet; do not add one until it is verified.

## Early API flow

1. `POST /api/v1/customers`
2. `POST /api/v1/orders` creates an order, shipment, and first tracking event.
3. `PATCH /api/v1/shipments/{id}/status` enforces the shipment transition graph.
4. `POST /api/v1/deliveries/assign` assigns available driver/vehicle resources.
5. `POST /api/v1/integrations/orders` requires `X-API-KEY` and returns an idempotent result for a repeated `externalOrderId`.

## Security note

The external integration route is API-key protected. Application login issues BCrypt-backed JWTs, and operational endpoints are role-gated. Customer/driver ownership scoping remains an explicitly tracked next hardening step.

## Architecture

```text
Browser dashboard → Spring REST controllers → services → JPA repositories → MySQL
External system → API-key filter → integration controller → order service → tracking event
```
