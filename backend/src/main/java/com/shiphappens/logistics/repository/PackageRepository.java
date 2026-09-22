package com.shiphappens.logistics.repository;

import com.shiphappens.logistics.entity.LogisticsOrder;
import com.shiphappens.logistics.entity.PackageItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PackageRepository extends JpaRepository<PackageItem, Long> {
    List<PackageItem> findByOrder(LogisticsOrder order);
    List<PackageItem> findByOrderId(Long orderId);

    @Query("select coalesce(sum(p.weightKg), 0) from PackageItem p where p.order = :order")
    BigDecimal totalWeight(@Param("order") LogisticsOrder order);
}
