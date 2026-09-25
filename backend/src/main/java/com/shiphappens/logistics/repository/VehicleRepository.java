package com.shiphappens.logistics.repository;

import com.shiphappens.logistics.entity.Statuses.VehicleStatus;
import com.shiphappens.logistics.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByStatus(VehicleStatus status);
    List<Vehicle> findByIdInAndStatus(List<Long> ids, VehicleStatus status);
    long countByStatus(VehicleStatus status);
}
