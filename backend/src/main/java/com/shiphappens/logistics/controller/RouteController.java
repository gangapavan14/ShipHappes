package com.shiphappens.logistics.controller;

import com.shiphappens.logistics.dto.Requests;
import com.shiphappens.logistics.dto.Responses.RoutePlanningResultResponse;
import com.shiphappens.logistics.dto.Responses.RouteResponse;
import com.shiphappens.logistics.dto.Responses.RouteStopResponse;
import com.shiphappens.logistics.route.RoutePlanningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RoutePlanningService routePlanningService;

    public RouteController(RoutePlanningService routePlanningService) {
        this.routePlanningService = routePlanningService;
    }

    @PostMapping("/plan")
    public ResponseEntity<RoutePlanningResultResponse> planRoutes(
            @RequestBody(required = false) Requests.PlanRoutes request) {
        RoutePlanningResultResponse result = routePlanningService.planRoutes(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/demo-scenario")
    public ResponseEntity<RoutePlanningResultResponse> loadExecutiveDemoScenario() {
        RoutePlanningResultResponse result = routePlanningService.loadExecutiveDemoScenario();
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    public List<RouteResponse> listRoutes() {
        return routePlanningService.listRoutes();
    }

    @GetMapping("/{id}")
    public RouteResponse getRoute(@PathVariable Long id) {
        return routePlanningService.getRoute(id);
    }

    @GetMapping("/{id}/stops")
    public List<RouteStopResponse> getRouteStops(@PathVariable Long id) {
        return routePlanningService.getRouteStops(id);
    }

    @PostMapping("/{id}/start")
    public RouteResponse startRoute(@PathVariable Long id) {
        return routePlanningService.startRoute(id);
    }

    @PostMapping("/{id}/complete")
    public RouteResponse completeRoute(@PathVariable Long id) {
        return routePlanningService.completeRoute(id);
    }

    @PostMapping("/{id}/cancel")
    public RouteResponse cancelRoute(@PathVariable Long id) {
        return routePlanningService.cancelRoute(id);
    }
}
