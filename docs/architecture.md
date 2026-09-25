Architecture

ShipHappens is a Spring Boot modular monolith. HTTP controllers receive validated DTOs, services enforce workflows and transactions, repositories contain database access, and Flyway owns schema evolution.

The system is intentionally kept as a modular monolith rather than being split into microservices.

High-Level Architecture

flowchart LR

    UI[HTML/CSS/JavaScript dashboard] --> API[Spring Boot REST API]

    EXT[External customer system] --> KEY[API-key filter]
    KEY --> API

    API --> S[Service layer]

    S --> SH[Shipment Service]
    S --> DEL[Delivery Service]
    S --> RP[Route Planning Service]
    S --> WH[Warehouse Service]
    S --> INT[Integration Service]
    S --> WHK[Webhook Service]

    SH --> R[JPA repositories]
    DEL --> R
    RP --> R
    WH --> R
    INT --> R
    WHK --> R

    R --> DB[(MySQL)]

    DB -. schema evolution .-> F[Flyway]

Layer Responsibilities

HTTP / External Systems
        |
        v
+----------------------+
| Controllers / Filters|
| - DTO validation     |
| - Authentication     |
| - HTTP responses     |
+----------+-----------+
           |
           v
+----------------------+
| Service Layer        |
| - Business rules     |
| - Workflows          |
| - Transactions       |
| - Route planning     |
+----------+-----------+
           |
           v
+----------------------+
| Repository Layer     |
| - JPA queries        |
| - Persistence        |
+----------+-----------+
           |
           v
+----------------------+
| MySQL                |
+----------------------+

Flyway
  |
  +--> owns database schema evolution

Controllers must not contain business logic.

Services own business workflows.

Repositories own database access.

Entities must not be exposed directly through REST APIs; use request and response DTOs.

Shipment Lifecycle

The shipment lifecycle represents the actual business state of a shipment.

flowchart LR

    CREATED --> ASSIGNED
    ASSIGNED --> PICKED_UP
    PICKED_UP --> IN_TRANSIT
    IN_TRANSIT --> AT_WAREHOUSE
    AT_WAREHOUSE --> OUT_FOR_DELIVERY
    OUT_FOR_DELIVERY --> DELIVERED

    CREATED --> CANCELLED

    PICKED_UP --> FAILED
    IN_TRANSIT --> FAILED
    AT_WAREHOUSE --> FAILED
    OUT_FOR_DELIVERY --> FAILED

    OUT_FOR_DELIVERY --> AT_WAREHOUSE
    FAILED --> OUT_FOR_DELIVERY

Shipment states

CREATED
ASSIGNED
PICKED_UP
IN_TRANSIT
AT_WAREHOUSE
OUT_FOR_DELIVERY
DELIVERED
FAILED
CANCELLED

The important distinction is:

CREATED
   |
   | Route planning / assignment
   v
ASSIGNED
   |
   | Physical pickup
   v
PICKED_UP

Route planning must not mark a shipment as PICKED_UP.

Route planning is a logical assignment operation. Pickup is a physical logistics event.

Core Modules

The backend is organized by business capability.

com.shiphappens.logistics
│
├── auth
├── customer
├── order
├── shipment
├── warehouse
├── driver
├── vehicle
├── delivery
├── route
├── tracking
├── integration
├── webhook
├── dashboard
├── common
└── config

Each business module may contain:

controller/
service/
repository/
entity/
dto/
mapper/
exception/

Only create the layers a module actually needs.

Core Domain Flow

The primary business flow is:

flowchart TD

    C[Customer] --> O[Order]
    O --> P[Package]
    O --> SH[Shipment]

    SH --> WH[Warehouse]
    SH --> RP[Route Planning]

    RP --> R[Route]
    R --> RS[Route Stops]
    RS --> SH

    R --> V[Vehicle]
    SH --> DA[Delivery Assignment]
    DA --> D[Driver]

    SH --> TE[Tracking Events]

    SH --> WB[Webhook Event]
    WB --> EXT[External Customer System]

Route Planning Architecture

What is a Route?

A Route is the output of the assignment/planning algorithm.

It is not manually created by an operator in the first version.

The route planner receives:

Pending shipments
+
Available vehicles
+
Vehicle capacities
+
Vehicle starting locations

and produces:

Route
    |
    +-- RouteStop 1
    +-- RouteStop 2
    +-- RouteStop 3
    +-- ...

The planner therefore answers two questions:

Which shipments should be assigned to which vehicle?

In what order should that vehicle visit those shipments?

Route Planning Flow

flowchart TD

    A[POST /api/v1/routes/plan] --> B[Route Planning Service]

    B --> C[Load CREATED shipments]
    B --> D[Load AVAILABLE vehicles]

    C --> E[Capacity-based assignment]
    D --> E

    E --> F[Group shipments by vehicle]

    F --> G[Route Sequencing Service]

    G --> H[Nearest-neighbor sequencing]
    H --> I[Haversine distance calculation]

    I --> J[Create Route]
    J --> K[Create ordered RouteStops]

    K --> L[Shipment CREATED -> ASSIGNED]
    K --> M[Vehicle AVAILABLE -> ASSIGNED]

    L --> N[Commit transaction]
    M --> N

    N --> O[Return planning result]

Route Domain Model

Route

Route
-------------------------
id
routeCode
vehicleId
status
totalDistanceKm
totalWeightKg
plannedAt
startedAt
completedAt
createdAt
updatedAt

Route status:

PLANNED
IN_PROGRESS
COMPLETED
CANCELLED

RouteStop

RouteStop
-------------------------
id
routeId
shipmentId
sequenceOrder
distanceFromPreviousKm
arrivalLatitude
arrivalLongitude
status

Route stop status:

PLANNED
VISITED
SKIPPED

Relationship:

Vehicle
   |
   +---- Route
            |
            +---- RouteStop ---- Shipment
            |
            +---- RouteStop ---- Shipment
            |
            +---- RouteStop ---- Shipment

A route owns the planned vehicle.

A route stop represents the shipment's position in that vehicle's route.

A shipment remains the central business entity.

Route Assignment

Route assignment handles the first part of optimization:

Shipment -> Vehicle

The vehicle must have sufficient remaining capacity.

Recommended first implementation:

First-Fit Decreasing

Load all CREATED shipments.

Sort shipments by weight descending.

Load all AVAILABLE vehicles.

Track remaining capacity for each vehicle.

For each shipment:

Find a vehicle with enough remaining capacity.

Assign the shipment.

Reduce that vehicle's remaining capacity.

If no vehicle can accept the shipment:

Leave it as CREATED.

Return it as an unassigned shipment.

Example:

Vehicle A = 1000 kg
Vehicle B = 500 kg

Shipments:
600 kg
400 kg
300 kg

Possible result:

Vehicle A
    600 + 400 = 1000 kg

Vehicle B
    300 kg

The algorithm must never produce:

assignedWeight > vehicle.capacityKg

Route Sequencing

After capacity assignment, shipments belonging to each vehicle are ordered geographically.

The first implementation uses the nearest-neighbor greedy heuristic.

Vehicle starting location
        |
        v
Find nearest shipment
        |
        v
Visit shipment
        |
        v
Find nearest remaining shipment
        |
        v
Visit shipment
        |
        v
Continue until no shipments remain

Pseudo-code:

currentLocation = vehicle.startLocation
remainingShipments = assignedShipments

while remainingShipments is not empty:

    nextShipment =
        shipment with minimum distance
        from currentLocation

    add nextShipment to ordered stops

    totalDistance +=
        distance(currentLocation,
                 nextShipment.destination)

    currentLocation =
        nextShipment.destination

    remove nextShipment

This is a legitimate route optimization heuristic.

It is not guaranteed to produce the mathematically optimal solution to the Vehicle Routing Problem.

The architecture deliberately keeps the algorithm replaceable so a more advanced optimizer can be introduced later.

Distance Calculation

The first version uses the Haversine formula.

DistanceCalculator
        |
        v
HaversineDistanceCalculator

The service must not contain geographic mathematics directly.

Recommended interface:

public interface DistanceCalculator {

    double calculateKm(
        double fromLatitude,
        double fromLongitude,
        double toLatitude,
        double toLongitude
    );
}

This allows a future road-distance implementation without changing route-planning business logic.

Possible future implementation:

RoadDistanceProvider
    |
    +-- Google Maps
    +-- Mapbox
    +-- OSRM / OpenStreetMap

The first version requires no external maps API.

Route Planning Services

Recommended service responsibilities:

RoutePlanningService
    |
    +-- orchestrates complete planning operation

RouteAssignmentService
    |
    +-- shipment -> vehicle assignment
    +-- capacity checks

RouteSequencingService
    |
    +-- determines stop order

DistanceCalculator
    |
    +-- calculates geographic distance

Recommended implementation:

RouteController
        |
        v
RoutePlanningService
        |
        +--> RouteAssignmentService
        |
        +--> RouteSequencingService
        |        |
        |        +--> DistanceCalculator
        |
        +--> RouteRepository
        |
        +--> RouteStopRepository
        |
        +--> ShipmentRepository
        |
        +--> VehicleRepository

This separation makes the algorithm independently testable.

Route Planning API

Plan Routes

POST /api/v1/routes/plan

Optional request:

{
  "vehicleIds": [1, 2, 3]
}

If vehicleIds is omitted, all eligible AVAILABLE vehicles may be considered.

Response:

{
  "routesCreated": 2,
  "shipmentsAssigned": 5,
  "shipmentsUnassigned": 1,
  "routes": [
    {
      "routeId": 101,
      "routeCode": "R-2026-001",
      "vehicleId": 4,
      "totalWeightKg": 820.0,
      "totalDistanceKm": 28.6,
      "stops": [
        {
          "shipmentId": 11,
          "sequenceOrder": 1,
          "distanceFromPreviousKm": 4.2
        },
        {
          "shipmentId": 14,
          "sequenceOrder": 2,
          "distanceFromPreviousKm": 6.7
        }
      ]
    }
  ],
  "unassignedShipments": [
    {
      "shipmentId": 17,
      "reason": "NO_VEHICLE_CAPACITY"
    }
  ]
}

Additional APIs:

GET  /api/v1/routes
GET  /api/v1/routes/{id}
GET  /api/v1/routes/{id}/stops

POST /api/v1/routes/{id}/start
POST /api/v1/routes/{id}/complete
POST /api/v1/routes/{id}/cancel

Route Planning Transaction

Route planning is a multi-step business operation and must be transactional.

BEGIN TRANSACTION

1. Load CREATED shipments
2. Load AVAILABLE vehicles
3. Calculate capacity assignment
4. Calculate stop sequence
5. Create Route records
6. Create RouteStop records
7. Change assigned shipments to ASSIGNED
8. Change assigned vehicles to ASSIGNED

COMMIT

If any required operation fails:

ROLLBACK

This prevents partial planning such as:

Route exists
but RouteStops do not

or:

Shipment = ASSIGNED
but no Route exists

The orchestration method should use:

@Transactional

Route Planning and Delivery Assignment

Route planning and driver assignment are related but represent different concepts.

Route
    =
vehicle's planned sequence of stops

DeliveryAssignment
    =
driver/vehicle responsibility for a shipment

Recommended flow:

Route Planning
       |
       v
Route created
       |
       +---- Vehicle selected
       |
       +---- RouteStops created
       |
       v
Driver assignment
       |
       v
DeliveryAssignment
       |
       v
PICKED_UP
       |
       v
IN_TRANSIT
       |
       v
OUT_FOR_DELIVERY
       |
       v
DELIVERED

Do not make route planning itself represent physical pickup.

Shipment Data Required for Routing

Shipments must contain destination coordinates.

Recommended fields:

id
trackingNumber
orderId
warehouseId
status
weightKg
destinationAddress
destinationLatitude
destinationLongitude
createdAt
updatedAt

Without latitude/longitude, geographic sequencing cannot be performed.

Vehicle should contain:

id
vehicleNumber
vehicleType
capacityKg
status
startLatitude
startLongitude

The vehicle's starting location is normally the depot/warehouse location.

Warehouse

Warehouse
-------------------------
id
warehouseCode
name
address
latitude
longitude
capacity
currentLoad
status

Status:

ACTIVE
INACTIVE

Rules:

Inactive warehouses cannot receive shipments.

currentLoad must not exceed capacity.

Receiving a shipment increases load.

Dispatching a shipment decreases load.

Warehouse coordinates can also serve as a vehicle's default route origin.

Driver

Driver
-------------------------
id
driverCode
name
phone
licenseNumber
status

Status:

AVAILABLE
ASSIGNED
OFF_DUTY
INACTIVE

Rules:

Only AVAILABLE drivers can be assigned.

Assignment changes driver to ASSIGNED.

Completing delivery returns driver to AVAILABLE.

Inactive drivers cannot be assigned.

Vehicle

Vehicle
-------------------------
id
vehicleNumber
vehicleType
capacityKg
status
startLatitude
startLongitude

Status:

AVAILABLE
ASSIGNED
MAINTENANCE
INACTIVE

Rules:

Only available vehicles can be planned.

Vehicle capacity must not be exceeded.

Vehicles under maintenance cannot be assigned.

A vehicle cannot have multiple active routes.

Completed vehicles become available again.

Delivery Assignment

DeliveryAssignment
-------------------------
id
shipmentId
driverId
vehicleId
assignedAt
pickupAt
outForDeliveryAt
deliveredAt
status

Status:

ASSIGNED
PICKED_UP
OUT_FOR_DELIVERY
DELIVERED
FAILED
CANCELLED

Rules:

One active delivery assignment per shipment.

Driver must be available.

Vehicle must be available.

Vehicle capacity must be sufficient.

Shipment must be in a valid lifecycle state.

Tracking

Every important shipment transition creates a tracking event.

TrackingEvent
-------------------------
id
shipmentId
eventType
status
description
location
createdAt
createdBy

Examples:

SHIPMENT_CREATED
ASSIGNED
PICKED_UP
ARRIVED_WAREHOUSE
DEPARTED_WAREHOUSE
OUT_FOR_DELIVERY
DELIVERED
DELIVERY_FAILED

Tracking history is immutable and ordered chronologically.

Tracking and Webhook Flow

flowchart LR

    A[Shipment status change]
    A --> B[Tracking Event]
    B --> C[Application Event]
    C --> D[Webhook Service]
    D --> E[External Customer]

A webhook failure must not roll back a successful shipment state transition.

External Integration

External customer systems submit orders through:

POST /api/v1/integrations/orders

Flow:

flowchart TD

    A[External System] --> B[API Key Filter]
    B --> C[Request ID]
    C --> D[Validation]
    D --> E[Idempotency Check]
    E --> F[External Order Mapper]
    F --> G[Order Service]
    G --> H[Shipment Service]
    H --> I[Integration Log]
    I --> J[Response]

Required capabilities:

API-key authentication

Request validation

Idempotency

External-to-internal mapping

Integration logging

Request IDs

Correct HTTP status codes

Idempotency

External systems may retry requests.

Use an external order identifier and/or idempotency key.

Conceptually:

External System
      |
      | first request
      v
Create Order
      |
      | retry
      v
Idempotency Check
      |
      v
Return existing result

The system must not create duplicate orders or shipments.

Webhooks

Webhook flow:

flowchart TD

    A[Shipment Event] --> B[Find Subscription]
    B --> C[Create Webhook Delivery]
    C --> D[Send HTTP POST]

    D --> E{2xx?}

    E -->|Yes| F[DELIVERED]
    E -->|No| G[RETRY]

    G --> H[Retry]
    H --> I[Retry]
    I --> J[FAILED]

Webhook delivery statuses:

PENDING
DELIVERED
RETRYING
FAILED

Webhook delivery should be asynchronous where practical.

Database Architecture

MySQL is the persistence layer.

Core tables:

users
customers
orders
packages
shipments
warehouses
drivers
vehicles
delivery_assignments
routes
route_stops
tracking_events
integration_logs
webhook_subscriptions
webhook_deliveries
idempotency_records

Key relationships:

Customer
   |
   +---- Order
           |
           +---- Package
           |
           +---- Shipment
                    |
                    +---- RouteStop ---- Route ---- Vehicle
                    |
                    +---- DeliveryAssignment ---- Driver
                    |
                    +---- TrackingEvent

Important Database Constraints

Use foreign keys and database constraints wherever practical.

Recommended unique values:

customer.email
vehicle.vehicleNumber
driver.driverCode
shipment.trackingNumber

For integrations:

externalSystem + externalOrderId

should be unique.

For routes:

routeId + sequenceOrder

should be unique.

Add indexes for common filters and joins:

shipments.status
shipments.trackingNumber
shipments.warehouseId
shipments.createdAt

routes.vehicleId
routes.status

routeStops.routeId
routeStops.shipmentId

trackingEvents.shipmentId
trackingEvents.createdAt

integrationLogs.requestId
integrationLogs.externalOrderId

webhookDeliveries.status
webhookDeliveries.nextRetryAt

API Structure

Base path:

/api/v1

Customers

POST   /customers
GET    /customers
GET    /customers/{id}
PUT    /customers/{id}
PATCH  /customers/{id}/status

Orders

POST   /orders
GET    /orders
GET    /orders/{id}
PUT    /orders/{id}
POST   /orders/{id}/cancel

Packages

POST /orders/{orderId}/packages
GET  /orders/{orderId}/packages

Shipments

POST  /shipments
GET   /shipments
GET   /shipments/{id}
PATCH /shipments/{id}/status
GET   /shipments/{id}/tracking

Warehouses

POST /warehouses
GET  /warehouses
GET  /warehouses/{id}
PUT  /warehouses/{id}

Drivers

POST /drivers
GET  /drivers
GET  /drivers/{id}
PUT  /drivers/{id}

Vehicles

POST /vehicles
GET  /vehicles
GET  /vehicles/{id}
PUT  /vehicles/{id}

Deliveries

POST  /deliveries/assign
GET   /deliveries
GET   /deliveries/{id}
PATCH /deliveries/{id}/status
POST  /deliveries/{id}/fail

Routes

POST /routes/plan
GET  /routes
GET  /routes/{id}
GET  /routes/{id}/stops

POST /routes/{id}/start
POST /routes/{id}/complete
POST /routes/{id}/cancel

Integrations

POST /integrations/orders

Dashboard

GET /dashboard/summary

Error Handling

Use centralized exception handling with:

@RestControllerAdvice

Response format:

{
  "timestamp": "2026-09-24T19:30:00Z",
  "status": 409,
  "error": "CONFLICT",
  "message": "Shipment cannot transition from DELIVERED to IN_TRANSIT",
  "path": "/api/v1/shipments/15/status",
  "requestId": "req-8f31c1"
}

Common status codes:

400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
500 Internal Server Error

Authentication

Internal users:

Username + Password
        |
        v
BCrypt verification
        |
        v
JWT
        |
        v
Role-based authorization

External integrations:

API Key
   |
   v
API-key authentication filter
   |
   v
External customer identity
   |
   v
Integration API

These authentication mechanisms remain separate.

Authorization

Roles:

ADMIN
OPERATIONS
CUSTOMER
DRIVER

Example access:

ADMIN
    full operational access

OPERATIONS
    orders
    shipments
    routes
    drivers
    vehicles
    warehouses
    tracking

CUSTOMER
    own orders
    own shipments
    tracking

DRIVER
    assigned routes
    assigned deliveries
    operational status updates

Authorization is enforced server-side.

Request IDs and Logging

Every request should have a request ID.

flowchart LR

    A[Incoming request] --> B{X-Request-ID exists?}

    B -->|Yes| C[Reuse ID]
    B -->|No| D[Generate ID]

    C --> E[MDC / Logging Context]
    D --> E

    E --> F[Controller]
    F --> G[Service]
    G --> H[Repository]

Return:

X-Request-ID: req-8f31c1

Never log:

passwords
JWTs
API keys
webhook secrets
sensitive personal information

Frontend Architecture

The frontend remains lightweight.

frontend/
├── index.html
├── login.html
├── dashboard.html
├── shipments.html
├── shipment-details.html
├── routes.html
├── vehicles.html
├── drivers.html
├── integrations.html
│
├── css/
│   └── styles.css
│
└── js/
    ├── api.js
    ├── auth.js
    ├── dashboard.js
    ├── shipments.js
    ├── routes.js
    └── common.js

The frontend consumes REST APIs.

Business rules remain on the backend.

Dashboard

Minimum operational metrics:

Total Shipments
Created
Assigned
In Transit
Out for Delivery
Delivered
Failed

Available Vehicles
Assigned Vehicles

Available Drivers
Assigned Drivers

Active Routes

Route information:

Route
Vehicle
Driver
Number of Stops
Total Weight
Total Distance
Status

Shipment detail:

Shipment information
Destination
Current status
Assigned route
Vehicle
Driver
Tracking timeline

Testing Architecture

Unit Tests

Prioritize:

RouteAssignmentService
RouteSequencingService
HaversineDistanceCalculator
ShipmentStateTransitionService
ExternalOrderMapper
IdempotencyService
WebhookRetryService

Route Algorithm Tests

One vehicle

3 shipments
1 vehicle
all fit

Expected:
1 route
3 stops

Capacity split

Vehicle A = 1000 kg
Vehicle B = 500 kg

Shipments:
600
400
300

Expected:
A = 600 + 400
B = 300

Oversized shipment

Vehicle = 500 kg
Shipment = 700 kg

Expected:
Shipment remains CREATED
Reason = NO_VEHICLE_CAPACITY

Nearest-neighbor

Given deterministic coordinates:

Depot → A → B → C

verify the generated sequence.

No vehicles

0 routes
all shipments remain CREATED

No pending shipments

0 routes
0 assignments

Repeated planning

Already ASSIGNED shipments must not be planned again.

Integration Tests

Test:

Controller
    ↓
Service
    ↓
Repository
    ↓
MySQL/Test Database

Required integration scenarios:

Complete shipment lifecycle

Route planning

Capacity assignment

Route sequencing

External integration

Duplicate integration request

Tracking events

Webhook persistence

Webhook retry

Database constraints

Docker Architecture

Local environment:

┌─────────────────────────┐
│ Spring Boot Application │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│       MySQL 8           │
└─────────────────────────┘

Docker Compose should support local development.

Production credentials must never be committed.

Use:

.env
.env.example

and ensure .env is ignored by Git.

Flyway

Flyway owns schema evolution.

Suggested migrations:

V1__create_users.sql
V2__create_customers.sql
V3__create_orders.sql
V4__create_packages.sql
V5__create_warehouses.sql
V6__create_drivers.sql
V7__create_vehicles.sql
V8__create_shipments.sql
V9__create_delivery_assignments.sql
V10__create_routes.sql
V11__create_route_stops.sql
V12__create_tracking_events.sql
V13__create_integrations.sql
V14__create_webhooks.sql
V15__add_indexes_and_constraints.sql
V16__seed_demo_data.sql

Do not modify an already-applied migration in a shared environment. Add a new migration instead.

Environment Configuration

Use environment variables for:

DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
INTEGRATION_API_KEY
WEBHOOK_TIMEOUT_MS
CORS_ALLOWED_ORIGINS

Local and production databases must be separate.

Deployment Architecture

The target production architecture remains:

Public Frontend
       |
       | HTTPS
       v
Spring Boot REST API
       |
       v
Managed MySQL
       |
       +---- External integrations
       |
       +---- Webhook endpoints

Do not introduce microservices for the first version.

Architecture Decisions

ADR-001 — Modular Monolith

Use one Spring Boot application.

Reason:

Portfolio project scale

Easier development

Easier testing

Easier deployment

Lower operational complexity

ADR-002 — Routes Are Generated

A route is the output of the planning algorithm.

Reason:

Routes depend on pending shipments

Routes depend on available vehicles

Route order is calculated

Avoids manually duplicated planning data

ADR-003 — Nearest-Neighbor Routing

Use nearest-neighbor for the first version.

Reason:

Simple

Deterministic

Easy to understand

Easy to test

Demonstrates actual optimization logic

Replaceable later

Limitation:

It is a heuristic, not guaranteed optimal.

ADR-004 — Capacity Assignment

Use first-fit-decreasing shipment assignment.

Reason:

Simple capacity-aware allocation

Easy to test

Appropriate for portfolio scale

Avoids overloading vehicles

ADR-005 — Haversine Distance

Use Haversine distance for the first version.

Reason:

No external API dependency

Deterministic

Easy local testing

Limitation:

It measures geographic distance rather than actual road distance.

ADR-006 — Planning Does Not Mean Pickup

Route planning changes:

CREATED → ASSIGNED

not:

CREATED → PICKED_UP

Reason:

Assignment and physical pickup are separate business events.

Extensibility

The route module must be designed so the algorithm can be replaced later.

Current:

RouteSequencer
      |
      +-- NearestNeighborRouteSequencer

Future:

RouteSequencer
      |
      +-- ORToolsRouteSequencer
      +-- RoadNetworkRouteSequencer

Recommended interface:

public interface RouteSequencer {

    List<Shipment> sequence(
        Vehicle vehicle,
        List<Shipment> shipments
    );
}

Similarly:

DistanceCalculator
      |
      +-- HaversineDistanceCalculator

can later become:

RoadDistanceProvider

without rewriting the route domain.

Complete Business Workflow

flowchart TD

    A[External Customer] --> B[POST /integrations/orders]
    B --> C[API Key Validation]
    C --> D[Validation + Idempotency]
    D --> E[Create Order]
    E --> F[Create Shipment]
    F --> G[Shipment = CREATED]

    G --> H[POST /routes/plan]

    H --> I[Find Pending Shipments]
    H --> J[Find Available Vehicles]

    I --> K[Capacity Assignment]
    J --> K

    K --> L[Nearest-Neighbor Sequencing]
    L --> M[Create Route + RouteStops]

    M --> N[Shipment = ASSIGNED]
    M --> O[Vehicle = ASSIGNED]

    N --> P[Assign Driver]
    P --> Q[PICKED_UP]
    Q --> R[IN_TRANSIT]
    R --> S[AT_WAREHOUSE]
    S --> T[OUT_FOR_DELIVERY]
    T --> U[DELIVERED]

    U --> V[Tracking Event]
    V --> W[Webhook]
    W --> X[External Customer]

    U --> Y[Vehicle = AVAILABLE]
    U --> Z[Driver = AVAILABLE]

Implementation Order

Phase 1 — Foundation

Spring Boot
MySQL
Flyway
Docker
Exception handling
Request IDs
Swagger

Phase 2 — Core Domain

Customer
Order
Package
Shipment
Warehouse

Phase 3 — Operations

Driver
Vehicle
DeliveryAssignment
TrackingEvent

Phase 4 — Route Planning

Route
RouteStop
DistanceCalculator
RouteAssignmentService
RouteSequencingService
RoutePlanningService
POST /routes/plan

Phase 5 — Security

Login
JWT
Roles
Authorization
API keys

Phase 6 — External Integration

External order API
Data mapping
Idempotency
Integration logs

Phase 7 — Webhooks

Subscriptions
Event generation
Delivery
Retry
Failure tracking

Phase 8 — Frontend

Dashboard
Shipments
Tracking
Routes
Vehicles
Drivers
Integrations

Phase 9 — Testing

Unit tests
Controller tests
Integration tests
Route algorithm tests
Webhook tests
Idempotency tests

Phase 10 — Deployment

Docker
Cloud backend
Managed MySQL
Static frontend
HTTPS
CI/CD

Final Architecture Principle

The project should be understood as:

Shipment lifecycle
       +
Operational assignment
       +
Route planning
       +
External integration
       +
Webhook communication

The most important relationship is:

CREATED Shipment
       |
       v
Route Planning Service
       |
       +--> Vehicle Assignment
       |
       +--> Stop Sequencing
       |
       +--> Distance Calculation
       |
       v
Route + RouteStops
       |
       v
Shipment = ASSIGNED
       |
       v
Delivery Execution
       |
       v
Shipment Lifecycle
       |
       v
Tracking + Webhooks

This keeps the existing ShipHappens architecture intact while making route planning a proper part of the operational domain instead of treating it as a separate application.

---

### Route Optimization & Executive Intelligence Architecture (V2.0)

#### 1. Optimization Heuristic Pipeline
```
[CREATED Shipments]
       |
       v
[First-Fit Decreasing Vehicle Bin-Packing]
  - Sort shipments by weight descending
  - Allocate to available vehicle meeting max payload capacity
       |
       v
[Greedy Nearest-Neighbor Seed Tour]
  - Origin: Central Depot (17.385044, 78.486671)
  - Distance: Haversine Great-Circle metric
       |
       v
[2-Opt Local Search TSP Edge-Swap Optimizer]
  - Iteratively swaps edge pairs (i, j) if Euclidean/Haversine delta < 0
  - Untangles crossed delivery paths to reach local optimum
  - Computes distance saved (km) and savings percentage (%)
       |
       v
[Carbon Emission Reductions]
  - CO₂ emission savings factor: 0.24 kg CO₂ per km avoided
  - Injected directly into RoutePlanningResultResponse DTO
```

#### 2. Executive Demo Scenario Loader (`POST /api/v1/routes/demo-scenario`)
- Automatically seeds high-volume enterprise customer (`Mojiro Executive Logistics`)
- Provisions 3 capacity-constrained vehicles (1000 kg, 700 kg, 1400 kg)
- Dispatches 13 multi-cluster orders across high-density corridors (Hitec City, Gachibowli, Banjara Hills, Financial District, Jubilee Hills, Begumpet)
- Executes bin-packing and 2-Opt sequencing in a single atomic transaction

#### 3. Client Presentation & Operations Visualizer
- **Minimalist Brand Architecture**: Clean Inter & JetBrains Mono typography, dark obsidian palette, hairline borders, no AI glow or emoji clutter.
- **Leaflet GIS Map**: Dark Carto cartography with depot and sequenced stop markers connected via color-coded polylines.
- **Live Delivery Simulation**: Interactive playback animating carrier progress along route legs with speed multiplier (1x, 2x, 5x) and floating telemetry HUD.
- **Customer Master Registry**: Dynamic customer selector in dispatch modals and dedicated customer portfolio interface.
- **Printable Driver Manifest**: PDF-ready operational run sheet with vehicle capacity utilization, delivery sequence, and signoff blocks.