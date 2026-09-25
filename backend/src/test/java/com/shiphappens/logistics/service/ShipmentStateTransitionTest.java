package com.shiphappens.logistics.service;

import com.shiphappens.logistics.dto.Requests;
import com.shiphappens.logistics.entity.Shipment;
import com.shiphappens.logistics.entity.Statuses.ShipmentStatus;
import com.shiphappens.logistics.exception.ApiException;
import com.shiphappens.logistics.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ShipmentStateTransitionTest {

    private ShipmentRepository shipmentRepository;
    private TrackingEventRepository trackingEventRepository;
    private LogisticsService logisticsService;

    @BeforeEach
    void setUp() {
        CustomerRepository customers = Mockito.mock(CustomerRepository.class);
        OrderRepository orders = Mockito.mock(OrderRepository.class);
        shipmentRepository = Mockito.mock(ShipmentRepository.class);
        DriverRepository drivers = Mockito.mock(DriverRepository.class);
        VehicleRepository vehicles = Mockito.mock(VehicleRepository.class);
        WarehouseRepository warehouses = Mockito.mock(WarehouseRepository.class);
        DeliveryAssignmentRepository assignments = Mockito.mock(DeliveryAssignmentRepository.class);
        trackingEventRepository = Mockito.mock(TrackingEventRepository.class);
        PackageRepository packages = Mockito.mock(PackageRepository.class);
        WebhookSubscriptionRepository webhookSubscriptions = Mockito.mock(WebhookSubscriptionRepository.class);
        ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);

        logisticsService = new LogisticsService(
                customers, orders, shipmentRepository, drivers, vehicles,
                warehouses, assignments, trackingEventRepository, packages,
                webhookSubscriptions, publisher
        );
    }

    private Shipment mockShipment(Long id, ShipmentStatus status) {
        Shipment s = new Shipment();
        s.setShipmentNumber("SH-" + id);
        s.setStatus(status);
        try {
            var idField = Shipment.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(s, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(shipmentRepository.findById(id)).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    @DisplayName("Lifecycle: CREATED -> ASSIGNED is allowed")
    void testCreatedToAssigned() {
        Shipment s = mockShipment(1L, ShipmentStatus.CREATED);
        Shipment updated = logisticsService.updateShipment(1L, new Requests.ShipmentStatus(ShipmentStatus.ASSIGNED, "Depot", "Route planned"));
        assertEquals(ShipmentStatus.ASSIGNED, updated.getStatus());
    }

    @Test
    @DisplayName("Lifecycle: ASSIGNED -> PICKED_UP is allowed")
    void testAssignedToPickedUp() {
        Shipment s = mockShipment(1L, ShipmentStatus.ASSIGNED);
        Shipment updated = logisticsService.updateShipment(1L, new Requests.ShipmentStatus(ShipmentStatus.PICKED_UP, "Depot", "Driver confirmed pickup"));
        assertEquals(ShipmentStatus.PICKED_UP, updated.getStatus());
    }

    @Test
    @DisplayName("Lifecycle: PICKED_UP -> IN_TRANSIT is allowed")
    void testPickedUpToInTransit() {
        Shipment s = mockShipment(1L, ShipmentStatus.PICKED_UP);
        Shipment updated = logisticsService.updateShipment(1L, new Requests.ShipmentStatus(ShipmentStatus.IN_TRANSIT, "Highways", "En route"));
        assertEquals(ShipmentStatus.IN_TRANSIT, updated.getStatus());
    }

    @Test
    @DisplayName("Lifecycle: IN_TRANSIT -> AT_WAREHOUSE is allowed")
    void testInTransitToAtWarehouse() {
        Shipment s = mockShipment(1L, ShipmentStatus.IN_TRANSIT);
        Shipment updated = logisticsService.updateShipment(1L, new Requests.ShipmentStatus(ShipmentStatus.AT_WAREHOUSE, "Hub", "Arrived"));
        assertEquals(ShipmentStatus.AT_WAREHOUSE, updated.getStatus());
    }

    @Test
    @DisplayName("Lifecycle: AT_WAREHOUSE -> OUT_FOR_DELIVERY is allowed")
    void testAtWarehouseToOutForDelivery() {
        Shipment s = mockShipment(1L, ShipmentStatus.AT_WAREHOUSE);
        Shipment updated = logisticsService.updateShipment(1L, new Requests.ShipmentStatus(ShipmentStatus.OUT_FOR_DELIVERY, "Last Mile", "Dispatched"));
        assertEquals(ShipmentStatus.OUT_FOR_DELIVERY, updated.getStatus());
    }

    @Test
    @DisplayName("Lifecycle: OUT_FOR_DELIVERY -> DELIVERED is allowed")
    void testOutForDeliveryToDelivered() {
        Shipment s = mockShipment(1L, ShipmentStatus.OUT_FOR_DELIVERY);
        Shipment updated = logisticsService.updateShipment(1L, new Requests.ShipmentStatus(ShipmentStatus.DELIVERED, "Customer Door", "Handed over"));
        assertEquals(ShipmentStatus.DELIVERED, updated.getStatus());
    }

    @Test
    @DisplayName("Lifecycle: DELIVERED -> IN_TRANSIT is forbidden (Throws 409 Conflict)")
    void testDeliveredToInTransitForbidden() {
        Shipment s = mockShipment(1L, ShipmentStatus.DELIVERED);
        ApiException ex = assertThrows(ApiException.class, () ->
                logisticsService.updateShipment(1L, new Requests.ShipmentStatus(ShipmentStatus.IN_TRANSIT, "Highways", "Illegal jump"))
        );
        assertEquals(409, ex.status().value());
    }
}
