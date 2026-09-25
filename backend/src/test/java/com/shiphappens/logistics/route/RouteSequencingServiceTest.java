package com.shiphappens.logistics.route;

import com.shiphappens.logistics.entity.Shipment;
import com.shiphappens.logistics.entity.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteSequencingServiceTest {

    private HaversineDistanceCalculator distanceCalculator;
    private NearestNeighborRouteSequencer sequencer;
    private RouteSequencingService sequencingService;

    @BeforeEach
    void setUp() {
        distanceCalculator = new HaversineDistanceCalculator();
        sequencer = new NearestNeighborRouteSequencer(distanceCalculator);
        sequencingService = new RouteSequencingService(sequencer, distanceCalculator);
    }

    private Shipment createShipmentWithCoords(Long id, String tracking, double lat, double lon) {
        Shipment s = new Shipment();
        s.setShipmentNumber("SH-" + id);
        s.setTrackingNumber(tracking);
        s.setDestinationLatitude(lat);
        s.setDestinationLongitude(lon);
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
    @DisplayName("Algorithm Test: Given Depot -> A -> B -> C, verify nearest-neighbor generated sequence")
    void testNearestNeighborSequencing() {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNumber("VH-DEMO");
        // Depot at (0.0, 0.0)
        vehicle.setStartLatitude(0.0);
        vehicle.setStartLongitude(0.0);

        // Point A: closest to Depot (0.1, 0.1)
        Shipment sA = createShipmentWithCoords(1L, "TRK-A", 0.1, 0.1);
        // Point B: closer to A than C (0.2, 0.2)
        Shipment sB = createShipmentWithCoords(2L, "TRK-B", 0.2, 0.2);
        // Point C: furthest away (0.5, 0.5)
        Shipment sC = createShipmentWithCoords(3L, "TRK-C", 0.5, 0.5);

        // Pass shipments in reverse/scrambled order: C, A, B
        RouteSequencingService.SequencedRoute result =
                sequencingService.planSequence(vehicle, List.of(sC, sA, sB));

        assertEquals(3, result.stops().size());
        assertEquals("TRK-A", result.stops().get(0).shipment().getTrackingNumber(), "First visited should be nearest A");
        assertEquals(1, result.stops().get(0).sequenceOrder());

        assertEquals("TRK-B", result.stops().get(1).shipment().getTrackingNumber(), "Second visited should be B");
        assertEquals(2, result.stops().get(1).sequenceOrder());

        assertEquals("TRK-C", result.stops().get(2).shipment().getTrackingNumber(), "Last visited should be C");
        assertEquals(3, result.stops().get(2).sequenceOrder());

        assertTrue(result.totalDistanceKm().doubleValue() > 0.0);
    }
}
