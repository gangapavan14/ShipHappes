package com.shiphappens.logistics.route;

import com.shiphappens.logistics.dto.Requests;
import com.shiphappens.logistics.dto.Responses.RoutePlanningResultResponse;
import com.shiphappens.logistics.dto.Responses.RouteResponse;
import com.shiphappens.logistics.entity.*;
import com.shiphappens.logistics.entity.Statuses.RouteStatus;
import com.shiphappens.logistics.entity.Statuses.ShipmentStatus;
import com.shiphappens.logistics.entity.Statuses.VehicleStatus;
import com.shiphappens.logistics.mapper.EntityMapper;
import com.shiphappens.logistics.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RoutePlanningServiceTest {

    private RouteRepository routeRepository;
    private RouteStopRepository routeStopRepository;
    private ShipmentRepository shipmentRepository;
    private VehicleRepository vehicleRepository;
    private TrackingEventRepository trackingEventRepository;
    private RouteAssignmentService assignmentService;
    private RouteSequencingService sequencingService;
    private EntityMapper entityMapper;
    private RoutePlanningService routePlanningService;

    @BeforeEach
    void setUp() {
        routeRepository = mock(RouteRepository.class);
        routeStopRepository = mock(RouteStopRepository.class);
        when(routeStopRepository.save(any(RouteStop.class))).thenAnswer(inv -> inv.getArgument(0));
        shipmentRepository = mock(ShipmentRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        trackingEventRepository = mock(TrackingEventRepository.class);
        assignmentService = mock(RouteAssignmentService.class);
        when(assignmentService.getWeight(any())).thenAnswer(inv -> {
            Shipment sh = inv.getArgument(0);
            return sh != null && sh.getWeightKg() != null ? sh.getWeightKg() : BigDecimal.ZERO;
        });
        sequencingService = mock(RouteSequencingService.class);
        entityMapper = new EntityMapper();

        routePlanningService = new RoutePlanningService(
                routeRepository,
                routeStopRepository,
                shipmentRepository,
                vehicleRepository,
                trackingEventRepository,
                assignmentService,
                sequencingService,
                entityMapper
        );
    }

    private Vehicle createVehicle(Long id, String number, String capacityKg) {
        Vehicle v = new Vehicle();
        v.setVehicleNumber(number);
        v.setVehicleType("Medium Van");
        v.setCapacityKg(new BigDecimal(capacityKg));
        v.setStatus(VehicleStatus.AVAILABLE);
        v.setStartLatitude(17.3850);
        v.setStartLongitude(78.4867);
        try {
            var f = Vehicle.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(v, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return v;
    }

    private Shipment createShipment(Long id, String tracking, String weightKg, ShipmentStatus status) {
        Shipment s = new Shipment();
        s.setShipmentNumber("SH-" + id);
        s.setTrackingNumber(tracking);
        s.setWeightKg(new BigDecimal(weightKg));
        s.setStatus(status);
        s.setDestinationLatitude(17.4155);
        s.setDestinationLongitude(78.4357);
        s.setDestinationAddress("Banjara Hills, Hyderabad");
        try {
            var f = Shipment.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(s, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return s;
    }

    @Test
    @DisplayName("Algorithm Test: Repeated planning must only consider CREATED shipments, ignoring ASSIGNED ones")
    void testRepeatedPlanningIgnoresAssignedShipments() {
        Shipment pendingShipment = createShipment(1L, "TRK-CREATED", "100.00", ShipmentStatus.CREATED);
        when(shipmentRepository.findByStatus(ShipmentStatus.CREATED)).thenReturn(List.of(pendingShipment));

        Vehicle vehicle = createVehicle(1L, "VH-01", "1000.00");
        when(vehicleRepository.findByStatus(VehicleStatus.AVAILABLE)).thenReturn(List.of(vehicle));

        when(assignmentService.assignShipments(any(), any())).thenReturn(
                new RouteAssignmentService.AssignmentResult(
                        Map.of(vehicle, List.of(pendingShipment)),
                        List.of()
                )
        );

        when(sequencingService.planSequence(any(), any())).thenReturn(
                new RouteSequencingService.SequencedRoute(
                        List.of(new RouteSequencingService.SequencedStop(pendingShipment, 1, new BigDecimal("5.50"), 17.4155, 78.4357)),
                        new BigDecimal("5.50")
                )
        );

        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> {
            Route r = invocation.getArgument(0);
            try {
                var f = Route.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(r, 99L);
            } catch (Exception ignored) {}
            return r;
        });

        RoutePlanningResultResponse result = routePlanningService.planRoutes(new Requests.PlanRoutes(List.of()));

        assertEquals(1, result.routesCreated());
        assertEquals(1, result.shipmentsAssigned());
        assertEquals(ShipmentStatus.ASSIGNED, pendingShipment.getStatus());

        verify(shipmentRepository).findByStatus(ShipmentStatus.CREATED);
        verify(shipmentRepository, never()).findByStatus(ShipmentStatus.ASSIGNED);
    }

    @Test
    @DisplayName("Lifecycle: startRoute transitions Route from PLANNED to IN_PROGRESS")
    void testStartRouteWorkflow() {
        Vehicle v = createVehicle(1L, "VH-01", "1000.00");
        Route route = new Route();
        route.setStatus(RouteStatus.PLANNED);
        route.setRouteCode("RT-100");
        route.setVehicle(v);

        when(routeRepository.findById(100L)).thenReturn(Optional.of(route));
        when(routeRepository.save(any(Route.class))).thenReturn(route);

        RouteResponse response = routePlanningService.startRoute(100L);

        assertEquals("IN_PROGRESS", response.status());
        assertNotNull(route.getStartedAt());
    }

    @Test
    @DisplayName("Lifecycle: cancelRoute cancels route, sets vehicle AVAILABLE, and resets shipments to CREATED")
    void testCancelRouteWorkflow() {
        Vehicle v = createVehicle(1L, "VH-01", "1000.00");
        v.setStatus(VehicleStatus.ASSIGNED);

        Route route = new Route();
        route.setStatus(RouteStatus.PLANNED);
        route.setRouteCode("RT-200");
        route.setVehicle(v);

        Shipment shipment = createShipment(10L, "TRK-10", "50.00", ShipmentStatus.ASSIGNED);
        RouteStop stop = new RouteStop();
        stop.setRoute(route);
        stop.setShipment(shipment);

        when(routeRepository.findById(200L)).thenReturn(Optional.of(route));
        when(routeStopRepository.findByRouteOrderBySequenceOrderAsc(route)).thenReturn(List.of(stop));
        when(routeRepository.save(any(Route.class))).thenReturn(route);

        RouteResponse response = routePlanningService.cancelRoute(200L);

        assertEquals("CANCELLED", response.status());
        assertEquals(VehicleStatus.AVAILABLE, v.getStatus());
        assertEquals(ShipmentStatus.CREATED, shipment.getStatus());
        verify(trackingEventRepository).save(any(TrackingEvent.class));
    }
}
