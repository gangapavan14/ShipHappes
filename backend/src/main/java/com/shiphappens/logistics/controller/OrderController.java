package com.shiphappens.logistics.controller;

import com.shiphappens.logistics.dto.Requests;
import com.shiphappens.logistics.dto.Responses.OrderResponse;
import com.shiphappens.logistics.dto.Responses.PackageResponse;
import com.shiphappens.logistics.entity.LogisticsOrder;
import com.shiphappens.logistics.entity.Shipment;
import com.shiphappens.logistics.mapper.EntityMapper;
import com.shiphappens.logistics.service.LogisticsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final LogisticsService service;
    private final EntityMapper mapper;

    public OrderController(LogisticsService service, EntityMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody Requests.CreateOrder request) {
        Shipment shipment = service.createOrder(request, null);
        LogisticsOrder order = shipment.getOrder();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toOrderResponse(order, service.getOrderPackages(order.getId())));
    }

    @GetMapping
    public List<OrderResponse> list(@RequestParam(required = false) Long customerId) {
        return service.listOrders(customerId).stream()
                .map(o -> mapper.toOrderResponse(o, service.getOrderPackages(o.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        LogisticsOrder order = service.getOrder(id);
        return mapper.toOrderResponse(order, service.getOrderPackages(order.getId()));
    }

    @PutMapping("/{id}")
    public OrderResponse update(@PathVariable Long id, @Valid @RequestBody Requests.UpdateOrder request) {
        LogisticsOrder order = service.updateOrder(id, request);
        return mapper.toOrderResponse(order, service.getOrderPackages(order.getId()));
    }

    @DeleteMapping("/{id}")
    public OrderResponse cancel(@PathVariable Long id) {
        LogisticsOrder order = service.cancelOrder(id);
        return mapper.toOrderResponse(order, service.getOrderPackages(order.getId()));
    }

    @GetMapping("/{id}/packages")
    public List<PackageResponse> getPackages(@PathVariable Long id) {
        return service.getOrderPackages(id).stream()
                .map(mapper::toPackageResponse)
                .toList();
    }
}
