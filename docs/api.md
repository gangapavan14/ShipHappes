# API guide

Base URL: `http://localhost:8080/api/v1`. Swagger is available at `/swagger-ui/index.html`.

Send `X-Request-ID` to trace a request; ShipHappens creates and returns one when absent. Error responses contain timestamp, status, error code, message, path, and request ID.

## Authentication

`POST /auth/login` accepts `{ "email", "password" }` and returns a JWT. Send it as `Authorization: Bearer <token>` for protected endpoints. Administrative registration is protected and intended for controlled setup only.

## Key workflows

- `POST /customers`: create customer (ADMIN/OPERATIONS).
- `POST /orders`: create order, packages, shipment, and the first tracking event.
- `PATCH /shipments/{id}/status`: moves a shipment only through supported transitions.
- `POST /deliveries/assign`: assigns an available driver and vehicle with sufficient capacity.
- `PATCH /deliveries/{id}/status`: records delivery workflow updates and releases driver/vehicle at delivered/failed terminal states.
- `POST /integrations/orders`: accepts an external order with `X-API-KEY`; a repeat `externalOrderId` returns idempotent success.
- `POST /webhooks/subscriptions`: creates an outbound subscription. Status changes trigger signed JSON delivery attempts.

Current access rules and unfinished ownership constraints are tracked in `agent-handoff.md`; do not expose the current development configuration publicly.
