package com.shiphappens.logistics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Responses {
    private Responses() {}

    public record CustomerResponse(
            Long id, String customerCode, String name, String email, String phone,
            String status, Instant createdAt) {}

    public record AddressResponse(
            Long id, String addressLine1, String addressLine2,
            String city, String state, String postalCode, String country) {}

    public record PackageResponse(
            Long id, String packageCode, String description,
            BigDecimal weightKg, BigDecimal lengthCm, BigDecimal widthCm,
            BigDecimal heightCm, BigDecimal declaredValue) {}

    public record OrderResponse(
            Long id, String orderNumber, String externalReference,
            Long customerId, String customerName,
            AddressResponse pickupAddress, AddressResponse deliveryAddress,
            String status, Instant createdAt, List<PackageResponse> packages) {}

    public record ShipmentResponse(
            Long id, String shipmentNumber, String trackingNumber,
            String status, Instant createdAt,
            Long orderId, String orderNumber,
            Long customerId, String customerName,
            AddressResponse deliveryAddress) {}

    public record TrackingEventResponse(
            Long id, String eventType, String status,
            String description, String location, Instant createdAt) {}

    public record DeliveryResponse(
            Long id, Long shipmentId, String shipmentNumber,
            Long driverId, String driverName,
            Long vehicleId, String vehicleNumber,
            String status, Instant assignedAt,
            Instant pickupAt, Instant outForDeliveryAt,
            Instant deliveredAt, String failureReason) {}

    public record WarehouseResponse(
            Long id, String warehouseCode, String name,
            String address, String city, String state,
            int capacity, int currentLoad, String status) {}

    public record DriverResponse(
            Long id, String driverCode, String name,
            String phone, String licenseNumber, String status) {}

    public record VehicleResponse(
            Long id, String vehicleNumber, String vehicleType,
            BigDecimal capacityKg, String status) {}

    public record DashboardResponse(
            long activeShipments, long inTransit, long outForDelivery,
            long delivered, long failed, long atWarehouse, long pickedUp) {}

    public record WebhookSubscriptionResponse(
            Long id, Long customerId, String targetUrl, boolean active) {}

    public record StatusSummaryResponse(String status, long count) {}

    public record DeliveryPerformanceResponse(
            long delivered, long failed, double successRate) {}

    public record WarehouseUtilizationResponse(
            Long warehouseId, String warehouseCode, String name,
            int capacity, int currentLoad, double utilizationPercent) {}
}

