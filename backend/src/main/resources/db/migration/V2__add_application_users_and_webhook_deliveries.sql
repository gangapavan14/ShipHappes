CREATE TABLE app_users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(30) NOT NULL,
  customer_id BIGINT NULL,
  driver_id BIGINT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  CONSTRAINT fk_user_driver FOREIGN KEY (driver_id) REFERENCES drivers(id)
);
CREATE TABLE webhook_delivery_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  event_id VARCHAR(80) NOT NULL,
  subscription_id BIGINT NOT NULL,
  shipment_id BIGINT NOT NULL,
  event_type VARCHAR(80) NOT NULL,
  attempt_number INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  http_status INT NULL,
  error_message VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_delivery_subscription FOREIGN KEY (subscription_id) REFERENCES webhook_subscriptions(id),
  CONSTRAINT fk_delivery_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
  INDEX idx_delivery_event (event_id), INDEX idx_delivery_shipment (shipment_id)
);
