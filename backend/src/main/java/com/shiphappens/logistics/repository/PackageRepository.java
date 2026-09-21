package com.shiphappens.logistics.repository;
import com.shiphappens.logistics.entity.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.math.BigDecimal; import java.util.*;
public interface PackageRepository extends JpaRepository<PackageItem,Long>{ List<PackageItem> findByOrder(LogisticsOrder order); @Query("select coalesce(sum(p.weightKg),0) from PackageItem p where p.order = :order") BigDecimal totalWeight(@Param("order") LogisticsOrder order); }
