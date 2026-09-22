package com.shiphappens.logistics.repository;

import com.shiphappens.logistics.entity.LogisticsOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<LogisticsOrder, Long> {
    Optional<LogisticsOrder> findByExternalReference(String reference);
    List<LogisticsOrder> findByCustomerId(Long customerId);
    List<LogisticsOrder> findAllByOrderByCreatedAtDesc();
}
