# External order mapping

| External field | Internal destination | Rule |
|---|---|---|
| `externalOrderId` | `orders.external_reference` | Direct, unique/idempotency key |
| `customerCode` | `customers.customer_code` | Active customer lookup |
| `pickup.*` | pickup address | Direct validated mapping |
| `delivery.*` | delivery address | Direct validated mapping |
| `packages[].weightKg` | package weight | Must be greater than zero |
| `packages[].declaredValue` | package declared value | Must be zero or greater |

The mapping is implemented in `ExternalOrderMapper`; integration orchestration and request logs belong in `IntegrationService`.
