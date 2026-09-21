# Architecture

ShipHappens is a Spring Boot modular monolith. HTTP controllers receive validated DTOs, services enforce workflows and transactions, repositories contain database access, and Flyway owns schema evolution.

```mermaid
flowchart LR
  UI[HTML/CSS/JavaScript dashboard] --> API[Spring Boot REST API]
  EXT[External customer system] --> KEY[API-key filter]
  KEY --> API
  API --> S[Service layer]
  S --> R[JPA repositories]
  R --> DB[(MySQL)]
```

## Shipment lifecycle

```mermaid
flowchart LR
  CREATED --> PICKED_UP --> IN_TRANSIT --> AT_WAREHOUSE --> OUT_FOR_DELIVERY --> DELIVERED
  CREATED --> CANCELLED
  PICKED_UP --> FAILED
  IN_TRANSIT --> FAILED
  AT_WAREHOUSE --> FAILED
  OUT_FOR_DELIVERY --> FAILED
  OUT_FOR_DELIVERY --> AT_WAREHOUSE
  FAILED --> OUT_FOR_DELIVERY
```
