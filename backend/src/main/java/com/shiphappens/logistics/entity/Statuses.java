package com.shiphappens.logistics.entity;

public final class Statuses {
  private Statuses() {}
  public enum CustomerStatus { ACTIVE, INACTIVE }
  public enum OrderStatus { CREATED, CONFIRMED, CANCELLED }
  public enum ShipmentStatus { CREATED, PICKED_UP, IN_TRANSIT, AT_WAREHOUSE, OUT_FOR_DELIVERY, DELIVERED, FAILED, CANCELLED }
  public enum DriverStatus { AVAILABLE, ASSIGNED, OFF_DUTY, INACTIVE }
  public enum VehicleStatus { AVAILABLE, ASSIGNED, MAINTENANCE, INACTIVE }
  public enum DeliveryStatus { ASSIGNED, PICKED_UP, OUT_FOR_DELIVERY, DELIVERED, FAILED, CANCELLED }
  public enum UserRole { ADMIN, OPERATIONS, CUSTOMER, DRIVER }
  public enum WebhookDeliveryStatus { SUCCESS, RETRYING, FAILED }
}
