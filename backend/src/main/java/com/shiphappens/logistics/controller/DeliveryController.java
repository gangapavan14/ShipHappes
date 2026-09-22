package com.shiphappens.logistics.controller;

import com.shiphappens.logistics.dto.Requests;
import com.shiphappens.logistics.dto.Responses.DeliveryResponse;
import com.shiphappens.logistics.dto.Responses.DriverResponse;
import com.shiphappens.logistics.dto.Responses.VehicleResponse;
import com.shiphappens.logistics.dto.Responses.WarehouseResponse;
import com.shiphappens.logistics.mapper.EntityMapper;
import com.shiphappens.logistics.service.LogisticsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class DeliveryController {

    private final LogisticsService service;
    private final EntityMapper mapper;

    public DeliveryController(LogisticsService service, EntityMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    // --- Deliveries ---

    @PostMapping("/deliveries/assign")
    public ResponseEntity<DeliveryResponse> assign(@Valid @RequestBody Requests.AssignDelivery request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toDeliveryResponse(service.assign(request)));
    }

    @PatchMapping("/deliveries/{id}/status")
    public DeliveryResponse updateDeliveryStatus(
            @PathVariable Long id,
            @Valid @RequestBody Requests.DeliveryStatus request) {
        return mapper.toDeliveryResponse(service.updateDelivery(id, request));
    }

    @GetMapping("/deliveries")
    public List<DeliveryResponse> listDeliveries() {
        return service.listDeliveries().stream()
                .map(mapper::toDeliveryResponse)
                .toList();
    }

    @GetMapping("/deliveries/{id}")
    public DeliveryResponse getDelivery(@PathVariable Long id) {
        return mapper.toDeliveryResponse(service.getDelivery(id));
    }

    // --- Drivers ---

    @GetMapping("/drivers")
    public List<DriverResponse> drivers() {
        return service.drivers().stream()
                .map(mapper::toDriverResponse)
                .toList();
    }

    @PostMapping("/drivers")
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody Requests.Driver request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toDriverResponse(service.createDriver(request)));
    }

    @GetMapping("/drivers/{id}")
    public DriverResponse getDriver(@PathVariable Long id) {
        return mapper.toDriverResponse(service.getDriver(id));
    }

    @PutMapping("/drivers/{id}")
    public DriverResponse updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody Requests.UpdateDriver request) {
        return mapper.toDriverResponse(service.updateDriver(id, request));
    }

    @PatchMapping("/drivers/{id}/status")
    public DriverResponse updateDriverStatus(
            @PathVariable Long id,
            @Valid @RequestBody Requests.DriverStatusUpdate request) {
        return mapper.toDriverResponse(service.updateDriverStatus(id, request.status()));
    }

    // --- Vehicles ---

    @GetMapping("/vehicles")
    public List<VehicleResponse> vehicles() {
        return service.vehicles().stream()
                .map(mapper::toVehicleResponse)
                .toList();
    }

    @PostMapping("/vehicles")
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody Requests.Vehicle request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toVehicleResponse(service.createVehicle(request)));
    }

    @GetMapping("/vehicles/{id}")
    public VehicleResponse getVehicle(@PathVariable Long id) {
        return mapper.toVehicleResponse(service.getVehicle(id));
    }

    @PutMapping("/vehicles/{id}")
    public VehicleResponse updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody Requests.UpdateVehicle request) {
        return mapper.toVehicleResponse(service.updateVehicle(id, request));
    }

    @PatchMapping("/vehicles/{id}/status")
    public VehicleResponse updateVehicleStatus(
            @PathVariable Long id,
            @Valid @RequestBody Requests.VehicleStatusUpdate request) {
        return mapper.toVehicleResponse(service.updateVehicleStatus(id, request.status()));
    }

    // --- Warehouses ---

    @GetMapping("/warehouses")
    public List<WarehouseResponse> warehouses() {
        return service.warehouses().stream()
                .map(mapper::toWarehouseResponse)
                .toList();
    }

    @PostMapping("/warehouses")
    public ResponseEntity<WarehouseResponse> createWarehouse(@Valid @RequestBody Requests.Warehouse request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toWarehouseResponse(service.createWarehouse(request)));
    }

    @GetMapping("/warehouses/{id}")
    public WarehouseResponse getWarehouse(@PathVariable Long id) {
        return mapper.toWarehouseResponse(service.getWarehouse(id));
    }

    @PutMapping("/warehouses/{id}")
    public WarehouseResponse updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody Requests.UpdateWarehouse request) {
        return mapper.toWarehouseResponse(service.updateWarehouse(id, request));
    }
}
