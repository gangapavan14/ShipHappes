package com.shiphappens.logistics.route;

import com.shiphappens.logistics.dto.Requests;
import com.shiphappens.logistics.dto.Responses.RoutePlanningResultResponse;
import com.shiphappens.logistics.dto.Responses.RouteResponse;
import com.shiphappens.logistics.dto.Responses.RouteStopResponse;
import com.shiphappens.logistics.dto.Responses.UnassignedShipmentResponse;
import com.shiphappens.logistics.entity.*;
import com.shiphappens.logistics.entity.Statuses.RouteStatus;
import com.shiphappens.logistics.entity.Statuses.RouteStopStatus;
import com.shiphappens.logistics.entity.Statuses.ShipmentStatus;
import com.shiphappens.logistics.entity.Statuses.VehicleStatus;
import com.shiphappens.logistics.exception.ApiException;
import com.shiphappens.logistics.mapper.EntityMapper;
import com.shiphappens.logistics.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class RoutePlanningService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final ShipmentRepository shipmentRepository;
    private final VehicleRepository vehicleRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final RouteAssignmentService assignmentService;
    private final RouteSequencingService sequencingService;
    private final EntityMapper entityMapper;

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    @Autowired
    public RoutePlanningService(
            RouteRepository routeRepository,
            RouteStopRepository routeStopRepository,
            ShipmentRepository shipmentRepository,
            VehicleRepository vehicleRepository,
            TrackingEventRepository trackingEventRepository,
            RouteAssignmentService assignmentService,
            RouteSequencingService sequencingService,
            EntityMapper entityMapper,
            CustomerRepository customerRepository,
            OrderRepository orderRepository) {
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.shipmentRepository = shipmentRepository;
        this.vehicleRepository = vehicleRepository;
        this.trackingEventRepository = trackingEventRepository;
        this.assignmentService = assignmentService;
        this.sequencingService = sequencingService;
        this.entityMapper = entityMapper;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    public RoutePlanningService(
            RouteRepository routeRepository,
            RouteStopRepository routeStopRepository,
            ShipmentRepository shipmentRepository,
            VehicleRepository vehicleRepository,
            TrackingEventRepository trackingEventRepository,
            RouteAssignmentService assignmentService,
            RouteSequencingService sequencingService,
            EntityMapper entityMapper) {
        this(routeRepository, routeStopRepository, shipmentRepository, vehicleRepository,
                trackingEventRepository, assignmentService, sequencingService, entityMapper, null, null);
    }

    @Transactional
    public RoutePlanningResultResponse planRoutes(Requests.PlanRoutes request) {
        // 1. Load CREATED shipments
        List<Shipment> createdShipments = shipmentRepository.findByStatus(ShipmentStatus.CREATED);

        // 2. Load AVAILABLE vehicles
        List<Vehicle> availableVehicles;
        if (request != null && request.vehicleIds() != null && !request.vehicleIds().isEmpty()) {
            availableVehicles = vehicleRepository.findByIdInAndStatus(request.vehicleIds(), VehicleStatus.AVAILABLE);
        } else {
            availableVehicles = vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);
        }

        // 3. Capacity assignment (First-Fit Decreasing)
        RouteAssignmentService.AssignmentResult assignmentResult =
                assignmentService.assignShipments(createdShipments, availableVehicles);

        List<RouteResponse> createdRouteResponses = new ArrayList<>();
        int totalAssignedShipments = 0;

        // 4. For each assigned vehicle, sequence stops and persist Route + RouteStops
        for (Map.Entry<Vehicle, List<Shipment>> entry : assignmentResult.vehicleAssignments().entrySet()) {
            Vehicle vehicle = entry.getKey();
            List<Shipment> vehicleShipments = entry.getValue();

            if (vehicleShipments.isEmpty()) {
                continue;
            }

            // Sequence stops geographically using nearest-neighbor
            RouteSequencingService.SequencedRoute sequenced =
                    sequencingService.planSequence(vehicle, vehicleShipments);

            BigDecimal totalWeight = BigDecimal.ZERO;
            for (Shipment s : vehicleShipments) {
                BigDecimal w = assignmentService.getWeight(s);
                if (w != null) {
                    totalWeight = totalWeight.add(w);
                }
            }

            // Create and persist Route
            Route route = new Route();
            route.setRouteCode("RT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            route.setVehicle(vehicle);
            route.setStatus(RouteStatus.PLANNED);
            route.setPlannedAt(Instant.now());
            route.setTotalDistanceKm(sequenced.totalDistanceKm());
            route.setTotalWeightKg(totalWeight);
            route = routeRepository.save(route);

            // Create and persist RouteStops
            List<RouteStopResponse> stopResponses = new ArrayList<>();
            for (RouteSequencingService.SequencedStop seqStop : sequenced.stops()) {
                Shipment s = seqStop.shipment();

                RouteStop stop = new RouteStop();
                stop.setRoute(route);
                stop.setShipment(s);
                stop.setSequenceOrder(seqStop.sequenceOrder());
                stop.setDistanceFromPreviousKm(seqStop.distanceFromPreviousKm());
                stop.setArrivalLatitude(seqStop.arrivalLatitude());
                stop.setArrivalLongitude(seqStop.arrivalLongitude());
                stop.setStatus(RouteStopStatus.PLANNED);
                stop = routeStopRepository.save(stop);

                // Transition shipment: CREATED -> ASSIGNED
                s.setStatus(ShipmentStatus.ASSIGNED);
                shipmentRepository.save(s);

                // Record tracking event
                recordTrackingEvent(s, "ASSIGNED",
                        "Shipment assigned to route " + route.getRouteCode() + " (Stop #" + stop.getSequenceOrder() + ")",
                        "Vehicle " + vehicle.getVehicleNumber());

                stopResponses.add(entityMapper.toRouteStopResponse(stop));
                totalAssignedShipments++;
            }

            // Transition vehicle: AVAILABLE -> ASSIGNED
            vehicle.setStatus(VehicleStatus.ASSIGNED);
            vehicleRepository.save(vehicle);

            RouteResponse baseRes = entityMapper.toRouteResponse(route, stopResponses);
            RouteResponse enriched = new RouteResponse(
                    baseRes.id(),
                    baseRes.routeCode(),
                    baseRes.vehicleId(),
                    baseRes.vehicleNumber(),
                    baseRes.vehicleType(),
                    baseRes.status(),
                    baseRes.totalDistanceKm(),
                    baseRes.totalWeightKg(),
                    baseRes.plannedAt(),
                    baseRes.startedAt(),
                    baseRes.completedAt(),
                    baseRes.createdAt(),
                    baseRes.updatedAt(),
                    baseRes.stops(),
                    sequenced.initialDistanceKm(),
                    sequenced.distanceSavedKm(),
                    sequenced.savingsPercentage(),
                    sequenced.co2SavedKg()
            );
            createdRouteResponses.add(enriched);
        }

        // Unassigned shipments response
        List<UnassignedShipmentResponse> unassignedResponses = new ArrayList<>();
        for (RouteAssignmentService.UnassignedShipment u : assignmentResult.unassignedShipments()) {
            unassignedResponses.add(new UnassignedShipmentResponse(
                    u.shipment().getId(),
                    u.shipment().getTrackingNumber(),
                    u.reason()
            ));
        }

        BigDecimal totalDistanceSaved = BigDecimal.ZERO;
        BigDecimal totalCo2Saved = BigDecimal.ZERO;
        for (RouteResponse r : createdRouteResponses) {
            if (r.distanceSavedKm() != null) totalDistanceSaved = totalDistanceSaved.add(r.distanceSavedKm());
            if (r.co2SavedKg() != null) totalCo2Saved = totalCo2Saved.add(r.co2SavedKg());
        }

        return new RoutePlanningResultResponse(
                createdRouteResponses.size(),
                totalAssignedShipments,
                unassignedResponses.size(),
                createdRouteResponses,
                unassignedResponses,
                totalDistanceSaved,
                totalCo2Saved
        );
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> listRoutes() {
        return routeRepository.findAllByOrderByPlannedAtDesc().stream()
                .map(entityMapper::toRouteResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RouteResponse getRoute(Long id) {
        Route route = findRouteById(id);
        return entityMapper.toRouteResponse(route);
    }

    @Transactional(readOnly = true)
    public List<RouteStopResponse> getRouteStops(Long routeId) {
        Route route = findRouteById(routeId);
        return routeStopRepository.findByRouteOrderBySequenceOrderAsc(route).stream()
                .map(entityMapper::toRouteStopResponse)
                .toList();
    }

    @Transactional
    public RouteResponse startRoute(Long id) {
        Route route = findRouteById(id);
        if (route.getStatus() != RouteStatus.PLANNED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_ROUTE_STATUS",
                    "Only PLANNED routes can be started. Current status: " + route.getStatus());
        }
        route.setStatus(RouteStatus.IN_PROGRESS);
        route.setStartedAt(Instant.now());
        route = routeRepository.save(route);
        return entityMapper.toRouteResponse(route);
    }

    @Transactional
    public RouteResponse completeRoute(Long id) {
        Route route = findRouteById(id);
        if (route.getStatus() != RouteStatus.IN_PROGRESS && route.getStatus() != RouteStatus.PLANNED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_ROUTE_STATUS",
                    "Cannot complete route in status: " + route.getStatus());
        }
        route.setStatus(RouteStatus.COMPLETED);
        route.setCompletedAt(Instant.now());

        // Mark planned stops as VISITED
        List<RouteStop> stops = routeStopRepository.findByRouteOrderBySequenceOrderAsc(route);
        for (RouteStop s : stops) {
            if (s.getStatus() == RouteStopStatus.PLANNED) {
                s.setStatus(RouteStopStatus.VISITED);
                routeStopRepository.save(s);
            }
        }

        // Return vehicle to AVAILABLE
        Vehicle vehicle = route.getVehicle();
        if (vehicle != null) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }

        route = routeRepository.save(route);
        return entityMapper.toRouteResponse(route);
    }

    @Transactional
    public RouteResponse cancelRoute(Long id) {
        Route route = findRouteById(id);
        if (route.getStatus() == RouteStatus.COMPLETED || route.getStatus() == RouteStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_ROUTE_STATUS",
                    "Route is already " + route.getStatus());
        }
        route.setStatus(RouteStatus.CANCELLED);

        // Revert assigned shipments back to CREATED if they were not yet physically picked up
        List<RouteStop> stops = routeStopRepository.findByRouteOrderBySequenceOrderAsc(route);
        for (RouteStop s : stops) {
            s.setStatus(RouteStopStatus.SKIPPED);
            routeStopRepository.save(s);

            Shipment shipment = s.getShipment();
            if (shipment != null && shipment.getStatus() == ShipmentStatus.ASSIGNED) {
                shipment.setStatus(ShipmentStatus.CREATED);
                shipmentRepository.save(shipment);

                recordTrackingEvent(shipment, "ROUTE_CANCELLED",
                        "Route " + route.getRouteCode() + " was cancelled. Shipment returned to pending queue.",
                        "Depot");
            }
        }

        // Return vehicle to AVAILABLE
        Vehicle vehicle = route.getVehicle();
        if (vehicle != null) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }

        route = routeRepository.save(route);
        return entityMapper.toRouteResponse(route);
    }

    private Route findRouteById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND", "Route not found with id " + id));
    }

    private void recordTrackingEvent(Shipment s, String type, String description, String location) {
        TrackingEvent e = new TrackingEvent();
        e.setShipment(s);
        e.setEventType(type);
        e.setStatus(s.getStatus().name());
        e.setDescription(description);
        e.setLocation(location);
        trackingEventRepository.save(e);
    }

    @Transactional
    public RoutePlanningResultResponse loadExecutiveDemoScenario() {
        if (customerRepository == null || orderRepository == null) {
            return planRoutes(null);
        }

        // 1. Ensure primary client exists
        Customer customer = customerRepository.findAll().stream().findFirst().orElseGet(() -> {
            Customer c = new Customer();
            c.setCustomerCode("CUST-MOJIRO");
            c.setName("Mojiro Logistics Enterprise");
            c.setEmail("fleet@mojiro.invalid");
            c.setPhone("+91 99000 88776");
            c.setStatus(Statuses.CustomerStatus.ACTIVE);
            return customerRepository.save(c);
        });

        // 2. Ensure 3 fleet vehicles with varying payload limits are ready
        ensureDemoVehicle("TS-09-EV-1001", "Electric Medium Van", new BigDecimal("1000.00"));
        ensureDemoVehicle("TS-09-TR-2002", "City Express Carrier", new BigDecimal("700.00"));
        ensureDemoVehicle("TS-09-HV-3003", "Heavy Corridor Hauler", new BigDecimal("1400.00"));

        // 3. Seed 9 distinct delivery orders across key Hyderabad corridors
        record DemoStop(String desc, double weight, String addr, double lat, double lon) {}
        List<DemoStop> stops = List.of(
            new DemoStop("Telecommunications Server Modules", 310.0, "Knowledge City, Hitec City, Hyderabad 500081", 17.4474, 78.3762),
            new DemoStop("Solar Inverters & Industrial Batteries", 240.0, "ISB Road, Gachibowli, Hyderabad 500032", 17.4191, 78.3429),
            new DemoStop("Precision Medical Diagnostic Kits", 180.0, "Road No. 12, Banjara Hills, Hyderabad 500034", 17.4156, 78.4358),
            new DemoStop("High-Density Computing Units", 350.0, "Financial District, Nanakramguda, Hyderabad 500032", 17.4150, 78.3380),
            new DemoStop("Substation Power Conditioning Spares", 280.0, "Paradise Circle, Secunderabad 500003", 17.4399, 78.4983),
            new DemoStop("Automated Drone Fleet Sensors", 95.0, "Kothaguda X Roads, Kondapur 500084", 17.4688, 78.3644),
            new DemoStop("Industrial CNC Precision Spares", 210.0, "Prakash Nagar, Begumpet 500016", 17.4435, 78.4682),
            new DemoStop("Fiber Optic Infrastructure Hardware", 140.0, "Cyber Towers, Madhapur 500081", 17.4483, 78.3915),
            new DemoStop("Enterprise Cold-Chain Biotech Kits", 160.0, "Road No. 36, Jubilee Hills 500033", 17.4319, 78.4073)
        );

        for (DemoStop s : stops) {
            LogisticsOrder order = new LogisticsOrder();
            order.setCustomer(customer);
            order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            order.setStatus(Statuses.OrderStatus.CONFIRMED);
            order = orderRepository.save(order);

            Shipment shipment = new Shipment();
            shipment.setOrder(order);
            shipment.setShipmentNumber("SHP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            shipment.setTrackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            shipment.setStatus(ShipmentStatus.CREATED);
            shipment.setWeightKg(BigDecimal.valueOf(s.weight));
            shipment.setDestinationLatitude(s.lat);
            shipment.setDestinationLongitude(s.lon);
            shipment.setDestinationAddress(s.addr);
            shipmentRepository.save(shipment);
        }

        // 4. Run First-Fit Decreasing and 2-Opt optimization
        return planRoutes(null);
    }

    private void ensureDemoVehicle(String plate, String type, BigDecimal cap) {
        Vehicle v = vehicleRepository.findAll().stream()
                .filter(veh -> plate.equalsIgnoreCase(veh.getVehicleNumber()))
                .findFirst()
                .orElseGet(() -> {
                    Vehicle nv = new Vehicle();
                    nv.setVehicleNumber(plate);
                    return nv;
                });
        v.setVehicleType(type);
        v.setCapacityKg(cap);
        v.setStatus(VehicleStatus.AVAILABLE);
        v.setStartLatitude(17.385044);
        v.setStartLongitude(78.486671);
        vehicleRepository.save(v);
    }
}
