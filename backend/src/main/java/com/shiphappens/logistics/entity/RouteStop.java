package com.shiphappens.logistics.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "route_stops")
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    @JsonIgnore
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Column(name = "distance_from_previous_km", nullable = false)
    private BigDecimal distanceFromPreviousKm = BigDecimal.ZERO;

    @Column(name = "arrival_latitude")
    private Double arrivalLatitude;

    @Column(name = "arrival_longitude")
    private Double arrivalLongitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Statuses.RouteStopStatus status = Statuses.RouteStopStatus.PLANNED;

    public Long getId() {
        return id;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(int sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public BigDecimal getDistanceFromPreviousKm() {
        return distanceFromPreviousKm;
    }

    public void setDistanceFromPreviousKm(BigDecimal distanceFromPreviousKm) {
        this.distanceFromPreviousKm = distanceFromPreviousKm;
    }

    public Double getArrivalLatitude() {
        return arrivalLatitude;
    }

    public void setArrivalLatitude(Double arrivalLatitude) {
        this.arrivalLatitude = arrivalLatitude;
    }

    public Double getArrivalLongitude() {
        return arrivalLongitude;
    }

    public void setArrivalLongitude(Double arrivalLongitude) {
        this.arrivalLongitude = arrivalLongitude;
    }

    public Statuses.RouteStopStatus getStatus() {
        return status;
    }

    public void setStatus(Statuses.RouteStopStatus status) {
        this.status = status;
    }
}
