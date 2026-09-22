package com.shiphappens.logistics.repository;

import com.shiphappens.logistics.entity.Shipment;
import com.shiphappens.logistics.entity.Statuses;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    @org.springframework.data.jpa.repository.Query("select s from Shipment s left join fetch s.order o left join fetch o.customer c where s.id = :id")
    java.util.Optional<Shipment> findByIdWithOrderAndCustomer(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("select s from Shipment s left join fetch s.order o left join fetch o.customer c left join fetch o.deliveryAddress order by s.createdAt desc")
    List<Shipment> findAllByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Query("select s from Shipment s left join fetch s.order o left join fetch o.customer c left join fetch o.deliveryAddress where s.status = :status order by s.createdAt desc")
    List<Shipment> findByStatusOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("status") Statuses.ShipmentStatus status);

    List<Shipment> findByStatus(Statuses.ShipmentStatus status);
    List<Shipment> findByOrderCustomerId(Long customerId);
    long countByStatus(Statuses.ShipmentStatus status);
}
