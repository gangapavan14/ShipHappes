package com.shiphappens.logistics.controller;

import com.shiphappens.logistics.dto.Requests;
import com.shiphappens.logistics.dto.Responses.CustomerResponse;
import com.shiphappens.logistics.mapper.EntityMapper;
import com.shiphappens.logistics.service.LogisticsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final LogisticsService service;
    private final EntityMapper mapper;

    public CustomerController(LogisticsService service, EntityMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody Requests.Customer request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toCustomerResponse(service.createCustomer(request)));
    }

    @GetMapping
    public List<CustomerResponse> list() {
        return service.listCustomers().stream()
                .map(mapper::toCustomerResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable Long id) {
        return mapper.toCustomerResponse(service.getCustomer(id));
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody Requests.UpdateCustomer request) {
        return mapper.toCustomerResponse(service.updateCustomer(id, request));
    }

    @PatchMapping("/{id}/status")
    public CustomerResponse updateStatus(@PathVariable Long id, @Valid @RequestBody Requests.CustomerStatusUpdate request) {
        return mapper.toCustomerResponse(service.updateCustomerStatus(id, request.status()));
    }
}
