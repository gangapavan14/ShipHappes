package com.shiphappens.logistics.controller;

import com.shiphappens.logistics.dto.Responses.DashboardResponse;
import com.shiphappens.logistics.entity.Statuses.ShipmentStatus;
import com.shiphappens.logistics.service.LogisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final LogisticsService service;

    public DashboardController(LogisticsService service) {
        this.service = service;
    }

    @GetMapping
    public DashboardResponse metrics() {
        long created = service.count(ShipmentStatus.CREATED);
        long pickedUp = service.count(ShipmentStatus.PICKED_UP);
        long inTransit = service.count(ShipmentStatus.IN_TRANSIT);
        long atWarehouse = service.count(ShipmentStatus.AT_WAREHOUSE);
        long outForDelivery = service.count(ShipmentStatus.OUT_FOR_DELIVERY);
        long delivered = service.count(ShipmentStatus.DELIVERED);
        long failed = service.count(ShipmentStatus.FAILED);
        long activeShipments = created + pickedUp + inTransit + atWarehouse + outForDelivery;

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
}
