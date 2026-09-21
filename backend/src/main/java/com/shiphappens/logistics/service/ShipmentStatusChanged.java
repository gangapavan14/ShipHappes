package com.shiphappens.logistics.service; import com.shiphappens.logistics.entity.Statuses.ShipmentStatus; public record ShipmentStatusChanged(Long shipmentId,ShipmentStatus status) {}
