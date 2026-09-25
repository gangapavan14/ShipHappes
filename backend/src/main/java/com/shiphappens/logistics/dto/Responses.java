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

    public record RouteStopResponse(
            Long id,
            Long routeId,
            Long shipmentId,
            String shipmentNumber,
            String trackingNumber,
            int sequenceOrder,
            BigDecimal distanceFromPreviousKm,
            Double arrivalLatitude,
            Double arrivalLongitude,
            String status,
            String destinationAddress,
            BigDecimal weightKg) {}

    public record RouteResponse(
            Long id,
            String routeCode,
            Long vehicleId,
            String vehicleNumber,
            String vehicleType,
            String status,
            BigDecimal totalDistanceKm,
            BigDecimal totalWeightKg,
            Instant plannedAt,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt,
            List<RouteStopResponse> stops,
            BigDecimal initialDistanceKm,
            BigDecimal distanceSavedKm,
            BigDecimal savingsPercentage,
            BigDecimal co2SavedKg) {
        public RouteResponse(Long id, String routeCode, Long vehicleId, String vehicleNumber, String vehicleType,
                             String status, BigDecimal totalDistanceKm, BigDecimal totalWeightKg, Instant plannedAt,
                             Instant startedAt, Instant completedAt, Instant createdAt, Instant updatedAt,
                             List<RouteStopResponse> stops) {
            this(id, routeCode, vehicleId, vehicleNumber, vehicleType, status, totalDistanceKm, totalWeightKg,
                 plannedAt, startedAt, completedAt, createdAt, updatedAt, stops, totalDistanceKm, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    public record UnassignedShipmentResponse(
            Long shipmentId,
            String trackingNumber,
            String reason) {}

    public record RoutePlanningResultResponse(
            int routesCreated,
            int shipmentsAssigned,
            int shipmentsUnassigned,
            List<RouteResponse> routes,
            List<UnassignedShipmentResponse> unassignedShipments,
            BigDecimal totalDistanceSavedKm,
            BigDecimal totalCo2SavedKg) {
        public RoutePlanningResultResponse(int routesCreated, int shipmentsAssigned, int shipmentsUnassigned,
                                           List<RouteResponse> routes, List<UnassignedShipmentResponse> unassignedShipments) {
            this(routesCreated, shipmentsAssigned, shipmentsUnassigned, routes, unassignedShipments, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    public record DashboardSummaryResponse(
            long totalShipments,
            long created,
            long assigned,
            long pickedUp,
            long inTransit,
            long atWarehouse,
            long outForDelivery,
            long delivered,
            long failed,
            long cancelled,
            long availableVehicles,
            long assignedVehicles,
            long availableDrivers,
            long assignedDrivers,
            long activeRoutes,
            List<RouteResponse> recentRoutes) {}
}

