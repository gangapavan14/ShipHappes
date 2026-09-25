package com.shiphappens.logistics.controller;

import com.shiphappens.logistics.dto.Responses.DashboardResponse;
import com.shiphappens.logistics.dto.Responses.DashboardSummaryResponse;
import com.shiphappens.logistics.dto.Responses.RouteResponse;
import com.shiphappens.logistics.entity.Statuses.DriverStatus;
import com.shiphappens.logistics.entity.Statuses.RouteStatus;
import com.shiphappens.logistics.entity.Statuses.ShipmentStatus;
import com.shiphappens.logistics.entity.Statuses.VehicleStatus;
import com.shiphappens.logistics.mapper.EntityMapper;
import com.shiphappens.logistics.repository.DriverRepository;
import com.shiphappens.logistics.repository.RouteRepository;
import com.shiphappens.logistics.repository.VehicleRepository;
import com.shiphappens.logistics.service.LogisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final LogisticsService service;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final EntityMapper mapper;

    public DashboardController(
            LogisticsService service,
            RouteRepository routeRepository,
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository,
            EntityMapper mapper) {
        this.service = service;
        this.routeRepository = routeRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.mapper = mapper;
    }

    @GetMapping
    public DashboardResponse metrics() {
        long created = service.count(ShipmentStatus.CREATED);
        long assigned = service.count(ShipmentStatus.ASSIGNED);
        long pickedUp = service.count(ShipmentStatus.PICKED_UP);
        long inTransit = service.count(ShipmentStatus.IN_TRANSIT);
        long atWarehouse = service.count(ShipmentStatus.AT_WAREHOUSE);
        long outForDelivery = service.count(ShipmentStatus.OUT_FOR_DELIVERY);
        long delivered = service.count(ShipmentStatus.DELIVERED);
        long failed = service.count(ShipmentStatus.FAILED);
        long activeShipments = created + assigned + pickedUp + inTransit + atWarehouse + outForDelivery;

        return new DashboardResponse(
                activeShipments,
                inTransit,
                outForDelivery,
                delivered,
                failed,
                atWarehouse,
                pickedUp
        );
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        long created = service.count(ShipmentStatus.CREATED);
        long assigned = service.count(ShipmentStatus.ASSIGNED);
        long pickedUp = service.count(ShipmentStatus.PICKED_UP);
        long inTransit = service.count(ShipmentStatus.IN_TRANSIT);
        long atWarehouse = service.count(ShipmentStatus.AT_WAREHOUSE);
        long outForDelivery = service.count(ShipmentStatus.OUT_FOR_DELIVERY);
        long delivered = service.count(ShipmentStatus.DELIVERED);
        long failed = service.count(ShipmentStatus.FAILED);
        long cancelled = service.count(ShipmentStatus.CANCELLED);
        long totalShipments = created + assigned + pickedUp + inTransit + atWarehouse + outForDelivery + delivered + failed + cancelled;

        long availableVehicles = vehicleRepository.countByStatus(VehicleStatus.AVAILABLE);
        long assignedVehicles = vehicleRepository.countByStatus(VehicleStatus.ASSIGNED);

        long availableDrivers = driverRepository.countByStatus(DriverStatus.AVAILABLE);
        long assignedDrivers = driverRepository.countByStatus(DriverStatus.ASSIGNED);

        long activeRoutes = routeRepository.countByStatus(RouteStatus.PLANNED) + routeRepository.countByStatus(RouteStatus.IN_PROGRESS);

        List<RouteResponse> recentRoutes = routeRepository.findAllByOrderByPlannedAtDesc().stream()
                .limit(5)
                .map(mapper::toRouteResponse)
                .toList();

        return new DashboardSummaryResponse(
                totalShipments,
                created,
                assigned,
                pickedUp,
                inTransit,
                atWarehouse,
                outForDelivery,
                delivered,
                failed,
                cancelled,
                availableVehicles,
                assignedVehicles,
                availableDrivers,
                assignedDrivers,
                activeRoutes,
                recentRoutes
        );
    }
}
