package com.shiphappens.logistics.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shipment_number")
    private String shipmentNumber;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private LogisticsOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    private Statuses.ShipmentStatus status = Statuses.ShipmentStatus.CREATED;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "destination_address")
    private String destinationAddress;

    @Column(name = "destination_latitude")
    private Double destinationLatitude;

    @Column(name = "destination_longitude")
    private Double destinationLongitude;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void created() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void updated() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getShipmentNumber() {
        return shipmentNumber;
    }

    public void setShipmentNumber(String shipmentNumber) {
        this.shipmentNumber = shipmentNumber;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    @JsonIgnore
    public LogisticsOrder getOrder() {
        return order;
    }

    public void setOrder(LogisticsOrder order) {
        this.order = order;
    }

    @JsonIgnore
    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Statuses.ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(Statuses.ShipmentStatus status) {
        this.status = status;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(Double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(Double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Double getEffectiveLatitude() {
        if (destinationLatitude != null) {
            return destinationLatitude;
        }
        if (order != null && order.getDeliveryAddress() != null) {
            return order.getDeliveryAddress().getLatitude();
        }
        return null;
    }

    public Double getEffectiveLongitude() {
        if (destinationLongitude != null) {
            return destinationLongitude;
        }
        if (order != null && order.getDeliveryAddress() != null) {
            return order.getDeliveryAddress().getLongitude();
        }
        return null;
    }

    public String getEffectiveDestinationAddress() {
        if (destinationAddress != null && !destinationAddress.isBlank()) {
            return destinationAddress;
        }
        if (order != null && order.getDeliveryAddress() != null) {
            Address a = order.getDeliveryAddress();
            return a.getAddressLine1() + ", " + a.getCity() + ", " + a.getState() + " " + a.getPostalCode();
        }
        return "";
    }
}
