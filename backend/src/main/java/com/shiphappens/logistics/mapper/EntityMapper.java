package com.shiphappens.logistics.mapper;

import com.shiphappens.logistics.dto.Responses.*;
import com.shiphappens.logistics.entity.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class EntityMapper {

    public CustomerResponse toCustomerResponse(Customer customer) {
        if (customer == null) return null;
        return new CustomerResponse(
                customer.getId(),
                customer.getCustomerCode(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getStatus() != null ? customer.getStatus().name() : null,
                customer.getCreatedAt()
        );
    }

    public AddressResponse toAddressResponse(Address address) {
        if (address == null) return null;
        return new AddressResponse(
                address.getId(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()
        );
    }

    public PackageResponse toPackageResponse(PackageItem pkg) {
        if (pkg == null) return null;
        return new PackageResponse(
                pkg.getId(),
                pkg.getPackageCode(),
                pkg.getDescription(),
                pkg.getWeightKg(),
                pkg.getLengthCm(),
                pkg.getWidthCm(),
                pkg.getHeightCm(),
                pkg.getDeclaredValue()
        );
    }

    public OrderResponse toOrderResponse(LogisticsOrder order, List<PackageItem> packages) {
        if (order == null) return null;
        List<PackageResponse> packageResponses = (packages != null)
                ? packages.stream().map(this::toPackageResponse).toList()
                : Collections.emptyList();

        Long customerId = order.getCustomer() != null ? order.getCustomer().getId() : null;
        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : null;

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getExternalReference(),
                customerId,
                customerName,
                toAddressResponse(order.getPickupAddress()),
                toAddressResponse(order.getDeliveryAddress()),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getCreatedAt(),
                packageResponses
        );
    }

    public ShipmentResponse toShipmentResponse(Shipment shipment) {
        if (shipment == null) return null;
        LogisticsOrder order = shipment.getOrder();
        Long orderId = order != null ? order.getId() : null;
        String orderNumber = order != null ? order.getOrderNumber() : null;
        Long customerId = (order != null && order.getCustomer() != null) ? order.getCustomer().getId() : null;
        String customerName = (order != null && order.getCustomer() != null) ? order.getCustomer().getName() : null;
        AddressResponse deliveryAddress = (order != null && order.getDeliveryAddress() != null)
                ? toAddressResponse(order.getDeliveryAddress())
                : null;

        return new ShipmentResponse(
                shipment.getId(),
                shipment.getShipmentNumber(),
                shipment.getTrackingNumber(),
                shipment.getStatus() != null ? shipment.getStatus().name() : null,
                shipment.getCreatedAt(),
                orderId,
                orderNumber,
                customerId,
                customerName,
                deliveryAddress
        );
    }

    public TrackingEventResponse toTrackingEventResponse(TrackingEvent event) {
        if (event == null) return null;
        return new TrackingEventResponse(
                event.getId(),
                event.getEventType(),
                event.getStatus(),
                event.getDescription(),
                event.getLocation(),
                event.getCreatedAt()
        );
    }

    public DeliveryResponse toDeliveryResponse(DeliveryAssignment assignment) {
        if (assignment == null) return null;
        Shipment shipment = assignment.getShipment();
        Driver driver = assignment.getDriver();
        Vehicle vehicle = assignment.getVehicle();

        Long shipmentId = shipment != null ? shipment.getId() : null;
        String shipmentNumber = shipment != null ? shipment.getShipmentNumber() : null;
        Long driverId = driver != null ? driver.getId() : null;
        String driverName = driver != null ? driver.getName() : null;
        Long vehicleId = vehicle != null ? vehicle.getId() : null;
        String vehicleNumber = vehicle != null ? vehicle.getVehicleNumber() : null;

        return new DeliveryResponse(
                assignment.getId(),
                shipmentId,
                shipmentNumber,
                driverId,
                driverName,
                vehicleId,
                vehicleNumber,
                assignment.getStatus() != null ? assignment.getStatus().name() : null,
                assignment.getAssignedAt(),
                assignment.getPickupAt(),
                assignment.getOutForDeliveryAt(),
                assignment.getDeliveredAt(),
                assignment.getFailureReason()
        );
    }

    public WarehouseResponse toWarehouseResponse(Warehouse warehouse) {
        if (warehouse == null) return null;
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getWarehouseCode(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getCity(),
                warehouse.getState(),
                warehouse.getCapacity(),
                warehouse.getCurrentLoad(),
                warehouse.getStatus()
        );
    }

    public DriverResponse toDriverResponse(Driver driver) {
        if (driver == null) return null;
        return new DriverResponse(
                driver.getId(),
                driver.getDriverCode(),
                driver.getName(),
                driver.getPhone(),
                driver.getLicenseNumber(),
                driver.getStatus() != null ? driver.getStatus().name() : null
        );
    }

    public VehicleResponse toVehicleResponse(Vehicle vehicle) {
        if (vehicle == null) return null;
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getVehicleNumber(),
                vehicle.getVehicleType(),
                vehicle.getCapacityKg(),
                vehicle.getStatus() != null ? vehicle.getStatus().name() : null
        );
    }

    public WebhookSubscriptionResponse toWebhookSubscriptionResponse(WebhookSubscription subscription) {
        if (subscription == null) return null;
        Long customerId = subscription.getCustomer() != null ? subscription.getCustomer().getId() : null;
        return new WebhookSubscriptionResponse(
                subscription.getId(),
                customerId,
                subscription.getTargetUrl(),
                subscription.isActive()
        );
    }
}
