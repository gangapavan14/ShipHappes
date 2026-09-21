# Troubleshooting

Use `X-Request-ID` from the response to locate relevant application log lines. For integration problems, inspect `integration_request_logs`, then the mapped order/shipment records. For outbound events, inspect `webhook_delivery_logs` by event ID; retry attempts are recorded without webhook secrets or payload signatures.

Local database: the ShipHappens Docker container maps host port `3307` to MySQL `3306` because `3306` is already occupied on this workstation. Run `docker compose ps` and `docker compose logs mysql` first. Run backend with `DB_URL=jdbc:mysql://localhost:3307/shiphappens`.
