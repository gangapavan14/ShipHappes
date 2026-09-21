CREATE TABLE customers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(160) NOT NULL,
  email VARCHAR(254) NOT NULL UNIQUE,
  phone VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_customer_code (customer_code)
);
CREATE TABLE addresses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT NULL,
  address_line1 VARCHAR(255) NOT NULL,
  address_line2 VARCHAR(255), city VARCHAR(100) NOT NULL, state VARCHAR(100) NOT NULL,
  postal_code VARCHAR(20) NOT NULL, country VARCHAR(100) NOT NULL,
  latitude DECIMAL(10,7), longitude DECIMAL(10,7),
  CONSTRAINT fk_address_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);
CREATE TABLE warehouses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, warehouse_code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(160) NOT NULL, address VARCHAR(255) NOT NULL, city VARCHAR(100) NOT NULL, state VARCHAR(100) NOT NULL,
  capacity INT NOT NULL, current_load INT NOT NULL DEFAULT 0, status VARCHAR(20) NOT NULL
);
CREATE TABLE drivers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, driver_code VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(160) NOT NULL, phone VARCHAR(30) NOT NULL, license_number VARCHAR(80) NOT NULL UNIQUE, status VARCHAR(20) NOT NULL
);
CREATE TABLE vehicles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, vehicle_number VARCHAR(40) NOT NULL UNIQUE,
  vehicle_type VARCHAR(50) NOT NULL, capacity_kg DECIMAL(12,2) NOT NULL, status VARCHAR(20) NOT NULL
);
CREATE TABLE orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, order_number VARCHAR(40) NOT NULL UNIQUE,
  external_reference VARCHAR(100) UNIQUE, customer_id BIGINT NOT NULL,
  pickup_address_id BIGINT NOT NULL, delivery_address_id BIGINT NOT NULL, status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  CONSTRAINT fk_order_pickup FOREIGN KEY (pickup_address_id) REFERENCES addresses(id),
  CONSTRAINT fk_order_delivery FOREIGN KEY (delivery_address_id) REFERENCES addresses(id),
  INDEX idx_order_created_at (created_at)
);
CREATE TABLE packages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, package_code VARCHAR(40) NOT NULL UNIQUE, order_id BIGINT NOT NULL,
  description VARCHAR(500) NOT NULL, weight_kg DECIMAL(12,2) NOT NULL, length_cm DECIMAL(12,2), width_cm DECIMAL(12,2), height_cm DECIMAL(12,2), declared_value DECIMAL(14,2) NOT NULL,
  CONSTRAINT fk_package_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
CREATE TABLE shipments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, shipment_number VARCHAR(40) NOT NULL UNIQUE, tracking_number VARCHAR(60) NOT NULL UNIQUE,
  order_id BIGINT NOT NULL UNIQUE, warehouse_id BIGINT NULL, status VARCHAR(30) NOT NULL, estimated_delivery_date DATE NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_shipment_order FOREIGN KEY (order_id) REFERENCES orders(id), CONSTRAINT fk_shipment_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
  INDEX idx_shipment_status (status), INDEX idx_shipment_tracking (tracking_number)
);
CREATE TABLE tracking_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, shipment_id BIGINT NOT NULL, event_type VARCHAR(50) NOT NULL, status VARCHAR(30) NOT NULL,
  description VARCHAR(500) NOT NULL, location VARCHAR(200), created_by VARCHAR(100), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_tracking_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id), INDEX idx_tracking_shipment (shipment_id, created_at)
);
CREATE TABLE delivery_assignments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, shipment_id BIGINT NOT NULL, driver_id BIGINT NOT NULL, vehicle_id BIGINT NOT NULL,
  assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, pickup_at TIMESTAMP NULL, out_for_delivery_at TIMESTAMP NULL, delivered_at TIMESTAMP NULL,
  status VARCHAR(30) NOT NULL, failure_reason VARCHAR(500),
  CONSTRAINT fk_assignment_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id), CONSTRAINT fk_assignment_driver FOREIGN KEY (driver_id) REFERENCES drivers(id), CONSTRAINT fk_assignment_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  INDEX idx_assignment_driver (driver_id), INDEX idx_assignment_vehicle (vehicle_id)
);
CREATE TABLE webhook_subscriptions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, customer_id BIGINT NULL, target_url VARCHAR(1000) NOT NULL, secret VARCHAR(255) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_webhook_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);
CREATE TABLE integration_request_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, request_id VARCHAR(80) NOT NULL UNIQUE, integration_name VARCHAR(100) NOT NULL, endpoint VARCHAR(255) NOT NULL,
  http_method VARCHAR(10) NOT NULL, external_reference VARCHAR(100), request_timestamp TIMESTAMP NOT NULL, response_timestamp TIMESTAMP NULL,
  http_status INT NULL, success BOOLEAN NOT NULL, error_code VARCHAR(100), error_message VARCHAR(500)
);
