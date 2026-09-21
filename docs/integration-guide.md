# Integration guide

Submit orders to `POST /api/v1/integrations/orders` with `X-API-KEY`. The API validates the payload, finds the active customer by code, maps the request into the core order workflow, creates packages/shipment/tracking data, and writes an integration request log without storing secrets.

The same `externalOrderId` is idempotent: the existing order is returned with `idempotent: true`.

Webhook subscriptions receive JSON status events carrying event ID, event type, timestamp, shipment ID, order ID, and status. ShipHappens sends an HMAC-SHA256 hex signature in `X-Webhook-Signature`. Non-success responses are retried up to three times and all attempts are stored in `webhook_delivery_logs`.

For local development, use `POST /api/v1/webhooks/mock-receiver` as a receiver URL only when it is reachable from the backend process.
