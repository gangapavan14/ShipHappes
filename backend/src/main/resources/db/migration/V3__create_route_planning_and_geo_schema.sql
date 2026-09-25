-- V3: Route Planning and Geographic Sequencing Schema
ALTER TABLE vehicles
  ADD COLUMN start_latitude DECIMAL(10,7) NULL AFTER status,
  ADD COLUMN start_longitude DECIMAL(10,7) NULL AFTER start_latitude;

ALTER TABLE warehouses
  ADD COLUMN latitude DECIMAL(10,7) NULL AFTER state,
  ADD COLUMN longitude DECIMAL(10,7) NULL AFTER latitude;

ALTER TABLE shipments
  ADD COLUMN weight_kg DECIMAL(12,2) NULL AFTER status,
  ADD COLUMN destination_address VARCHAR(255) NULL AFTER weight_kg,
  ADD COLUMN destination_latitude DECIMAL(10,7) NULL AFTER destination_address,
  ADD COLUMN destination_longitude DECIMAL(10,7) NULL AFTER destination_latitude;

CREATE TABLE routes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  route_code VARCHAR(40) NOT NULL UNIQUE,
  vehicle_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL,
  total_distance_km DECIMAL(10,2) NOT NULL DEFAULT 0.0,
  total_weight_kg DECIMAL(12,2) NOT NULL DEFAULT 0.0,
  planned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  started_at TIMESTAMP NULL,
  completed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_route_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  INDEX idx_routes_vehicle (vehicle_id),
  INDEX idx_routes_status (status)
);

CREATE TABLE route_stops (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  route_id BIGINT NOT NULL,
  shipment_id BIGINT NOT NULL,
  sequence_order INT NOT NULL,
  distance_from_previous_km DECIMAL(10,2) NOT NULL DEFAULT 0.0,
  arrival_latitude DECIMAL(10,7) NULL,
  arrival_longitude DECIMAL(10,7) NULL,
  status VARCHAR(30) NOT NULL,
  CONSTRAINT fk_stop_route FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE,
  CONSTRAINT fk_stop_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
  CONSTRAINT uq_route_sequence UNIQUE (route_id, sequence_order),
  INDEX idx_stops_route (route_id),
  INDEX idx_stops_shipment (shipment_id)
);

-- Seed default depot coordinates for demo warehouses & vehicles
UPDATE warehouses SET latitude = 17.385044, longitude = 78.486671 WHERE latitude IS NULL;
UPDATE vehicles SET start_latitude = 17.385044, start_longitude = 78.486671 WHERE start_latitude IS NULL;
