package com.shiphappens.logistics.controller;

import com.shiphappens.logistics.dto.Requests;
import com.shiphappens.logistics.dto.Responses.PackageResponse;
import com.shiphappens.logistics.dto.Responses.ShipmentResponse;
import com.shiphappens.logistics.dto.Responses.TrackingEventResponse;
import com.shiphappens.logistics.entity.Statuses.ShipmentStatus;
import com.shiphappens.logistics.mapper.EntityMapper;
import com.shiphappens.logistics.service.LogisticsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private final LogisticsService service;
    private final EntityMapper mapper;

    public ShipmentController(LogisticsService service, EntityMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ShipmentResponse> list(
            @RequestParam(required = false) ShipmentStatus status,
            @RequestParam(required = false) Long customerId) {
        return service.listShipments(status, customerId).stream()
                .map(mapper::toShipmentResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ShipmentResponse get(@PathVariable Long id) {
        return mapper.toShipmentResponse(service.findShipment(id));
    }

    @PatchMapping("/{id}/status")
    public ShipmentResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody Requests.ShipmentStatus request) {
        return mapper.toShipmentResponse(service.updateShipment(id, request));
    }

    @GetMapping("/{id}/tracking")
    public List<TrackingEventResponse> tracking(@PathVariable Long id) {
        return service.tracking(id).stream()
                .map(mapper::toTrackingEventResponse)
                .toList();
    }

    @GetMapping("/{id}/packages")
    public List<PackageResponse> getPackages(@PathVariable Long id) {
        return service.getShipmentPackages(id).stream()
                .map(mapper::toPackageResponse)
                .toList();
    }
}
