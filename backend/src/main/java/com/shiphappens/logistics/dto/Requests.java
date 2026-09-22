package com.shiphappens.logistics.dto;

import com.shiphappens.logistics.entity.Statuses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public final class Requests {
    private Requests() {}

    public record Customer(
            @NotBlank String customerCode,
            @NotBlank String name,
            @Email @NotBlank String email,
            @Pattern(regexp = "^[+0-9() -]{7,30}$", message = "phone must be a valid phone number") String phone) {}

    public record UpdateCustomer(
            @NotBlank String name,
            @Email @NotBlank String email,
            @Pattern(regexp = "^[+0-9() -]{7,30}$") String phone) {}

    public record CustomerStatusUpdate(@NotNull Statuses.CustomerStatus status) {}

    public record Address(
            @NotBlank String addressLine1, String addressLine2,
            @NotBlank String city, @NotBlank String state,
            @NotBlank String postalCode, @NotBlank String country) {}

    public record Package(
            @NotBlank String description,
            @NotNull @DecimalMin(value = "0.01") BigDecimal weightKg,
            @DecimalMin(value = "0.01") BigDecimal lengthCm,
            @DecimalMin(value = "0.01") BigDecimal widthCm,
            @DecimalMin(value = "0.01") BigDecimal heightCm,
            @NotNull @DecimalMin("0.0") BigDecimal declaredValue) {}

    public record CreateOrder(
            @NotNull Long customerId,
            @Valid @NotNull Address pickup,
            @Valid @NotNull Address delivery,
            @NotEmpty List<@Valid Package> packages) {}

    public record UpdateOrder(
            @Valid @NotNull Address pickup,
            @Valid @NotNull Address delivery) {}

    public record ShipmentStatus(
            @NotNull Statuses.ShipmentStatus status,
            String location, String description, Long warehouseId) {
        // backward-compat constructor for internal callers that don't set warehouseId
        public ShipmentStatus(Statuses.ShipmentStatus status, String location, String description) {
            this(status, location, description, null);
        }
    }

    public record AssignDelivery(
            @NotNull Long shipmentId,
            @NotNull Long driverId,
            @NotNull Long vehicleId) {}

    public record DeliveryStatus(
            @NotNull Statuses.DeliveryStatus status,
            String failureReason) {}

    public record DeliveryFail(
            @NotBlank String failureReason) {}

    public record Warehouse(
            @NotBlank String warehouseCode, @NotBlank String name,
            @NotBlank String address, @NotBlank String city, @NotBlank String state,
            @Positive int capacity, @NotBlank String status) {}

    public record UpdateWarehouse(
            @NotBlank String name, @NotBlank String address,
            @NotBlank String city, @NotBlank String state,
            @Positive int capacity, @NotBlank String status) {}

    public record Driver(
            @NotBlank String driverCode, @NotBlank String name,
            @Pattern(regexp = "^[+0-9() -]{7,30}$") String phone,
            @NotBlank String licenseNumber) {}

    public record UpdateDriver(
            @NotBlank String name,
            @Pattern(regexp = "^[+0-9() -]{7,30}$") String phone) {}

    public record Vehicle(
            @NotBlank String vehicleNumber, @NotBlank String vehicleType,
            @NotNull @DecimalMin("0.01") BigDecimal capacityKg) {}

    public record UpdateVehicle(
            @NotBlank String vehicleType,
            @NotNull @DecimalMin("0.01") BigDecimal capacityKg) {}

    public record ExternalOrder(
            @NotBlank String externalOrderId,
            @NotBlank String customerCode,
            @Valid @NotNull Address pickup,
            @Valid @NotNull Address delivery,
            @NotEmpty List<@Valid Package> packages) {}
    public record DriverStatusUpdate(@NotNull Statuses.DriverStatus status) {}

    public record VehicleStatusUpdate(@NotNull Statuses.VehicleStatus status) {}

    public record WebhookSubscription(
            Long customerId,
            @NotBlank @Pattern(regexp = "https?://.+") String targetUrl,
            @NotBlank @Size(min = 16) String secret) {}
}

