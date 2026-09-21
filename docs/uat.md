# UAT scenarios

| ID | Scenario | Expected result |
|---|---|---|
| UAT-001 | Create customer | HTTP 201; unique code enforced |
| UAT-002 | Create order | HTTP 201; packages/shipment/tracking event created |
| UAT-003 | Submit duplicate external order | Existing workflow returned; no duplicate rows |
| UAT-004 | Assign unavailable driver | HTTP 409 with clear business error |
| UAT-005 | Assign overweight shipment | HTTP 409 capacity error |
| UAT-006 | Attempt DELIVERED → IN_TRANSIT | HTTP 409 transition error |
| UAT-007 | Deliver assignment | Shipment/delivery events and resources reconciled (final implementation test required) |
| UAT-008 | Fail delivery | Failure reason/event/webhook are recorded (final implementation test required) |
