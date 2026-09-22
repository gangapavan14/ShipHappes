package com.shiphappens.logistics.controller;

import com.shiphappens.logistics.dto.Responses.DeliveryPerformanceResponse;
import com.shiphappens.logistics.dto.Responses.StatusSummaryResponse;
import com.shiphappens.logistics.dto.Responses.WarehouseUtilizationResponse;
import com.shiphappens.logistics.entity.Statuses.ShipmentStatus;
import com.shiphappens.logistics.service.LogisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final LogisticsService service;

    public ReportController(LogisticsService service) {
        this.service = service;
    }

    @GetMapping("/shipments/status-summary")
    public List<StatusSummaryResponse> statusSummary() {
        List<StatusSummaryResponse> result = new ArrayList<>();
        for (ShipmentStatus s : ShipmentStatus.values()) {
            result.add(new StatusSummaryResponse(s.name(), service.count(s)));
        }
        return result;
    }

    @GetMapping("/delivery-performance")
    public DeliveryPerformanceResponse deliveryPerformance() {
        long delivered = service.count(ShipmentStatus.DELIVERED);
        long failed = service.count(ShipmentStatus.FAILED);
        double successRate = (delivered + failed == 0)
                ? 0.0
                : Math.round(delivered * 10000.0 / (delivered + failed)) / 100.0;
        return new DeliveryPerformanceResponse(delivered, failed, successRate);
    }

    @GetMapping("/warehouses/utilization")
    public List<WarehouseUtilizationResponse> warehouseUtilization() {
        return service.warehouses().stream()
                .map(w -> {
                    double percent = w.getCapacity() > 0
                            ? Math.round((w.getCurrentLoad() * 10000.0) / w.getCapacity()) / 100.0
                            : 0.0;
                    return new WarehouseUtilizationResponse(
                            w.getId(),
                            w.getWarehouseCode(),
                            w.getName(),
                            w.getCapacity(),
                            w.getCurrentLoad(),
                            percent
                    );
                })
                .toList();
    }
}
