package com.shiphappens.logistics.repository;

import com.shiphappens.logistics.entity.Route;
import com.shiphappens.logistics.entity.Statuses.RouteStatus;
import com.shiphappens.logistics.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    Optional<Route> findByRouteCode(String routeCode);
    List<Route> findByStatus(RouteStatus status);
    List<Route> findByVehicle(Vehicle vehicle);
    List<Route> findAllByOrderByPlannedAtDesc();
    long countByStatus(RouteStatus status);
}
