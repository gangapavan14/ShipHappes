package com.shiphappens.logistics.repository;

import com.shiphappens.logistics.entity.Driver;
import com.shiphappens.logistics.entity.Statuses.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    List<Driver> findByStatus(DriverStatus status);
    long countByStatus(DriverStatus status);
}
