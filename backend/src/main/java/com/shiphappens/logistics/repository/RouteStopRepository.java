package com.shiphappens.logistics.repository;

import com.shiphappens.logistics.entity.Route;
import com.shiphappens.logistics.entity.RouteStop;
import com.shiphappens.logistics.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {
    List<RouteStop> findByRouteOrderBySequenceOrderAsc(Route route);
    Optional<RouteStop> findByShipment(Shipment shipment);
    List<RouteStop> findByShipmentIn(List<Shipment> shipments);
}
