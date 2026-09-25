package com.shiphappens.logistics.route;

import com.shiphappens.logistics.entity.Shipment;
import com.shiphappens.logistics.entity.Vehicle;
import com.shiphappens.logistics.repository.PackageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteAssignmentServiceTest {

    private PackageRepository packageRepository;
    private RouteAssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        packageRepository = Mockito.mock(PackageRepository.class);
        assignmentService = new RouteAssignmentService(packageRepository);
    }

    private Vehicle createVehicle(Long id, String number, String capacityKg) {
        Vehicle v = new Vehicle();
        v.setVehicleNumber(number);
        v.setCapacityKg(new BigDecimal(capacityKg));
        // Use reflection or direct field access if needed, or simulate ID via setter or hash
        try {
            var idField = Vehicle.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(v, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return v;
    }

    private Shipment createShipment(Long id, String tracking, String weightKg) {
        Shipment s = new Shipment();
        s.setShipmentNumber("SH-" + id);
        s.setTrackingNumber(tracking);
        s.setWeightKg(new BigDecimal(weightKg));
        try {
            var idField = Shipment.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(s, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return s;
    }

    @Test
    @DisplayName("Algorithm Test 1: One vehicle, 3 shipments all fit -> 1 route with 3 shipments")
    void testOneVehicleAllFit() {
        Vehicle v = createVehicle(1L, "VH-01", "1000.00");
        List<Shipment> shipments = List.of(
                createShipment(1L, "TRK-01", "200.00"),
                createShipment(2L, "TRK-02", "300.00"),
                createShipment(3L, "TRK-03", "400.00")
        );

        RouteAssignmentService.AssignmentResult result = assignmentService.assignShipments(shipments, List.of(v));

        assertEquals(1, result.vehicleAssignments().size());
        assertEquals(3, result.vehicleAssignments().get(v).size());
        assertTrue(result.unassignedShipments().isEmpty());
    }

    @Test
    @DisplayName("Algorithm Test 2: Capacity split - Vehicle A=1000kg, B=500kg, Shipments: 600, 400, 300 -> A gets 600+400, B gets 300")
    void testCapacitySplit() {
        Vehicle vA = createVehicle(1L, "VH-A", "1000.00");
        Vehicle vB = createVehicle(2L, "VH-B", "500.00");

        Shipment s600 = createShipment(1L, "TRK-600", "600.00");
        Shipment s400 = createShipment(2L, "TRK-400", "400.00");
        Shipment s300 = createShipment(3L, "TRK-300", "300.00");

        RouteAssignmentService.AssignmentResult result =
                assignmentService.assignShipments(List.of(s600, s400, s300), List.of(vA, vB));

        assertEquals(2, result.vehicleAssignments().size());
        assertEquals(2, result.vehicleAssignments().get(vA).size(), "Vehicle A should receive 600 and 400");
        assertEquals(1, result.vehicleAssignments().get(vB).size(), "Vehicle B should receive 300");
        assertTrue(result.unassignedShipments().isEmpty());
    }

    @Test
    @DisplayName("Algorithm Test 3: Oversized shipment - Vehicle 500kg, Shipment 700kg -> unassigned with NO_VEHICLE_CAPACITY")
    void testOversizedShipment() {
        Vehicle v = createVehicle(1L, "VH-01", "500.00");
        Shipment s = createShipment(1L, "TRK-700", "700.00");

        RouteAssignmentService.AssignmentResult result =
                assignmentService.assignShipments(List.of(s), List.of(v));

        assertTrue(result.vehicleAssignments().isEmpty());
        assertEquals(1, result.unassignedShipments().size());
        assertEquals("NO_VEHICLE_CAPACITY", result.unassignedShipments().get(0).reason());
    }

    @Test
    @DisplayName("Algorithm Test 4: No vehicles available -> 0 assignments, unassigned with NO_AVAILABLE_VEHICLES")
    void testNoVehicles() {
        Shipment s = createShipment(1L, "TRK-01", "100.00");
        RouteAssignmentService.AssignmentResult result =
                assignmentService.assignShipments(List.of(s), List.of());

        assertTrue(result.vehicleAssignments().isEmpty());
        assertEquals(1, result.unassignedShipments().size());
        assertEquals("NO_AVAILABLE_VEHICLES", result.unassignedShipments().get(0).reason());
    }

    @Test
    @DisplayName("Algorithm Test 5: No pending shipments -> 0 assignments, 0 unassigned")
    void testNoShipments() {
        Vehicle v = createVehicle(1L, "VH-01", "1000.00");
        RouteAssignmentService.AssignmentResult result =
                assignmentService.assignShipments(List.of(), List.of(v));

        assertTrue(result.vehicleAssignments().isEmpty());
        assertTrue(result.unassignedShipments().isEmpty());
    }
}
