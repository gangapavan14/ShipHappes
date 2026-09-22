package com.shiphappens.logistics.service;

import com.shiphappens.logistics.dto.Requests;
import com.shiphappens.logistics.entity.*;
import com.shiphappens.logistics.entity.Statuses.*;
import com.shiphappens.logistics.exception.ApiException;
import com.shiphappens.logistics.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class LogisticsService {

    private final CustomerRepository customers;
    private final OrderRepository orders;
    private final ShipmentRepository shipments;
    private final DriverRepository drivers;
    private final VehicleRepository vehicles;
    private final WarehouseRepository warehouses;
    private final DeliveryAssignmentRepository assignments;
    private final TrackingEventRepository events;
    private final PackageRepository packages;
    private final WebhookSubscriptionRepository webhookSubscriptions;
    private final ApplicationEventPublisher publisher;

    public LogisticsService(
            CustomerRepository customers,
            OrderRepository orders,
            ShipmentRepository shipments,
            DriverRepository drivers,
            VehicleRepository vehicles,
            WarehouseRepository warehouses,
            DeliveryAssignmentRepository assignments,
            TrackingEventRepository events,
            PackageRepository packages,
            WebhookSubscriptionRepository webhookSubscriptions,
            ApplicationEventPublisher publisher) {
        this.customers = customers;
        this.orders = orders;
        this.shipments = shipments;
        this.drivers = drivers;
        this.vehicles = vehicles;
        this.warehouses = warehouses;
        this.assignments = assignments;
        this.events = events;
        this.packages = packages;
        this.webhookSubscriptions = webhookSubscriptions;
        this.publisher = publisher;
    }

    // --- Customer Operations ---

    @Transactional
    public Customer createCustomer(Requests.Customer request) {
        if (customers.existsByCustomerCode(request.customerCode())) {
            throw conflict("Customer code already exists");
        }
        Customer c = new Customer();
        c.setCustomerCode(request.customerCode());
        c.setName(request.name());
        c.setEmail(request.email());
        c.setPhone(request.phone());
        return customers.save(c);
    }

    @Transactional(readOnly = true)
    public Customer getCustomer(Long id) {
        return customers.findById(id).orElseThrow(() -> notFound("Customer"));
    }

    @Transactional(readOnly = true)
    public List<Customer> listCustomers() {
        return customers.findAll();
    }

    @Transactional
    public Customer updateCustomer(Long id, Requests.UpdateCustomer request) {
        Customer c = getCustomer(id);
        c.setName(request.name());
        c.setEmail(request.email());
        c.setPhone(request.phone());
        return customers.save(c);
    }

    @Transactional
    public Customer updateCustomerStatus(Long id, CustomerStatus status) {
        Customer c = getCustomer(id);
        c.setStatus(status);
        return customers.save(c);
    }

    @Transactional(readOnly = true)
    public Customer customerByCode(String code) {
        return customers.findByCustomerCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "No active customer exists for customerCode " + code));
    }

    // --- Order Operations ---

    @Transactional
    public Shipment createOrder(Requests.CreateOrder request, String externalReference) {
        Customer c = customers.findById(request.customerId()).orElseThrow(() -> notFound("Customer"));
        if (c.getStatus() != CustomerStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INACTIVE_CUSTOMER", "Inactive customers cannot create orders");
        }

        LogisticsOrder order = new LogisticsOrder();
        order.setCustomer(c);
        order.setExternalReference(externalReference);
        order.setOrderNumber(code("ORD"));
        order.setPickupAddress(toAddress(request.pickup()));
        order.setDeliveryAddress(toAddress(request.delivery()));
        order = orders.save(order);

        for (Requests.Package requestPackage : request.packages()) {
            packages.save(toPackage(order, requestPackage));
        }

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setShipmentNumber(code("SHP"));
        shipment.setTrackingNumber(code("TRK"));
        shipment = shipments.save(shipment);

        recordEvent(shipment, "SHIPMENT_CREATED", "Shipment created", null);
        return shipment;
    }

    @Transactional(readOnly = true)
    public LogisticsOrder getOrder(Long id) {
        return orders.findById(id).orElseThrow(() -> notFound("Order"));
    }

    @Transactional(readOnly = true)
    public List<LogisticsOrder> listOrders(Long customerId) {
        if (customerId != null) {
            return orders.findByCustomerId(customerId);
        }
        return orders.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public LogisticsOrder updateOrder(Long id, Requests.UpdateOrder request) {
        LogisticsOrder order = getOrder(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw conflict("Cannot update cancelled order");
        }
        if (order.getPickupAddress() != null) {
            updateAddress(order.getPickupAddress(), request.pickup());
        } else {
            order.setPickupAddress(toAddress(request.pickup()));
        }
        if (order.getDeliveryAddress() != null) {
            updateAddress(order.getDeliveryAddress(), request.delivery());
        } else {
            order.setDeliveryAddress(toAddress(request.delivery()));
        }
        return orders.save(order);
    }

    @Transactional
    public LogisticsOrder cancelOrder(Long id) {
        LogisticsOrder order = getOrder(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        order.setStatus(OrderStatus.CANCELLED);
        // If shipment is in early status, cancel it too
        shipments.findAll().stream()
                .filter(s -> s.getOrder() != null && s.getOrder().getId().equals(id))
                .findFirst()
                .ifPresent(s -> {
                    if (s.getStatus() == ShipmentStatus.CREATED) {
                        s.setStatus(ShipmentStatus.CANCELLED);
                        recordEvent(s, "SHIPMENT_CANCELLED", "Shipment cancelled as order was cancelled", null);
                        publisher.publishEvent(new ShipmentStatusChanged(s.getId(), ShipmentStatus.CANCELLED));
                    }
                });
        return orders.save(order);
    }

    @Transactional(readOnly = true)
    public List<PackageItem> getOrderPackages(Long orderId) {
        LogisticsOrder order = getOrder(orderId);
        return packages.findByOrder(order);
    }

    @Transactional(readOnly = true)
    public Optional<LogisticsOrder> orderByExternalReference(String reference) {
        return orders.findByExternalReference(reference);
    }

    // --- Shipment Operations ---

    @Transactional(readOnly = true)
    public Shipment findShipment(Long id) {
        return shipments.findById(id).orElseThrow(() -> notFound("Shipment"));
    }

    @Transactional(readOnly = true)
    public List<Shipment> listShipments(ShipmentStatus status, Long customerId) {
        if (customerId != null) {
            return shipments.findByOrderCustomerId(customerId);
        }
        if (status != null) {
            return shipments.findByStatusOrderByCreatedAtDesc(status);
        }
        return shipments.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Shipment updateShipment(Long id, Requests.ShipmentStatus request) {
        Shipment shipment = findShipment(id);
        ShipmentStatus current = shipment.getStatus();
        if (!allowed(current, request.status())) {
            throw conflict("Cannot transition shipment from " + current + " to " + request.status());
        }
        shipment.setStatus(request.status());
        if (request.warehouseId() != null) {
            Warehouse w = warehouses.findById(request.warehouseId()).orElseThrow(() -> notFound("Warehouse"));
            shipment.setWarehouse(w);
        }
        recordEvent(
                shipment,
                "SHIPMENT_" + request.status(),
                request.description() == null ? "Shipment status changed to " + request.status() : request.description(),
                request.location()
        );
        publisher.publishEvent(new ShipmentStatusChanged(shipment.getId(), request.status()));
        return shipment;
    }

    @Transactional(readOnly = true)
    public List<TrackingEvent> tracking(Long id) {
        return events.findByShipmentOrderByCreatedAtAsc(findShipment(id));
    }

    @Transactional(readOnly = true)
    public List<PackageItem> getShipmentPackages(Long shipmentId) {
        Shipment shipment = findShipment(shipmentId);
        if (shipment.getOrder() == null) {
            return Collections.emptyList();
        }
        return packages.findByOrder(shipment.getOrder());
    }

    @Transactional(readOnly = true)
    public long count(ShipmentStatus status) {
        return shipments.countByStatus(status);
    }

    // --- Delivery Assignment Operations ---

    @Transactional
    public DeliveryAssignment assign(Requests.AssignDelivery request) {
        Shipment shipment = findShipment(request.shipmentId());
        if (shipment.getStatus() != ShipmentStatus.CREATED && shipment.getStatus() != ShipmentStatus.AT_WAREHOUSE) {
            throw conflict("Shipment must be CREATED or AT_WAREHOUSE before assignment");
        }
        if (assignments.existsByShipmentAndStatusIn(shipment, List.of(DeliveryStatus.ASSIGNED, DeliveryStatus.PICKED_UP, DeliveryStatus.OUT_FOR_DELIVERY))) {
            throw conflict("Shipment already has an active delivery assignment");
        }
        Driver driver = drivers.findById(request.driverId()).orElseThrow(() -> notFound("Driver"));
        Vehicle vehicle = vehicles.findById(request.vehicleId()).orElseThrow(() -> notFound("Vehicle"));
        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            throw conflict("Driver is not available");
        }
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw conflict("Vehicle is not available");
        }
        if (packages.totalWeight(shipment.getOrder()).compareTo(vehicle.getCapacityKg()) > 0) {
            throw conflict("Vehicle capacity is insufficient for this shipment");
        }
        driver.setStatus(DriverStatus.ASSIGNED);
        vehicle.setStatus(VehicleStatus.ASSIGNED);
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setShipment(shipment);
        assignment.setDriver(driver);
        assignment.setVehicle(vehicle);
        return assignments.save(assignment);
    }

    @Transactional
    public DeliveryAssignment updateDelivery(Long id, Requests.DeliveryStatus request) {
        DeliveryAssignment assignment = assignments.findById(id).orElseThrow(() -> notFound("Delivery assignment"));
        DeliveryStatus current = assignment.getStatus();
        if (!validDeliveryTransition(current, request.status())) {
            throw conflict("Cannot transition delivery from " + current + " to " + request.status());
        }
        Shipment shipment = assignment.getShipment();
        if (request.status() == DeliveryStatus.PICKED_UP) {
            updateShipment(shipment.getId(), new Requests.ShipmentStatus(ShipmentStatus.PICKED_UP, null, "Driver confirmed pickup"));
            assignment.setPickupAt(Instant.now());
        }
        if (request.status() == DeliveryStatus.OUT_FOR_DELIVERY) {
            updateShipment(shipment.getId(), new Requests.ShipmentStatus(ShipmentStatus.OUT_FOR_DELIVERY, null, "Driver is out for delivery"));
            assignment.setOutForDeliveryAt(Instant.now());
        }
        if (request.status() == DeliveryStatus.DELIVERED) {
            updateShipment(shipment.getId(), new Requests.ShipmentStatus(ShipmentStatus.DELIVERED, null, "Delivery completed"));
            assignment.setDeliveredAt(Instant.now());
            if (assignment.getDriver() != null) assignment.getDriver().setStatus(DriverStatus.AVAILABLE);
            if (assignment.getVehicle() != null) assignment.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        }
        if (request.status() == DeliveryStatus.FAILED) {
            updateShipment(shipment.getId(), new Requests.ShipmentStatus(ShipmentStatus.FAILED, null, request.failureReason() == null ? "Delivery failed" : request.failureReason()));
            assignment.setFailureReason(request.failureReason());
            if (assignment.getDriver() != null) assignment.getDriver().setStatus(DriverStatus.AVAILABLE);
            if (assignment.getVehicle() != null) assignment.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        }
        assignment.setStatus(request.status());
        return assignment;
    }

    @Transactional(readOnly = true)
    public DeliveryAssignment getDelivery(Long id) {
        return assignments.findById(id).orElseThrow(() -> notFound("Delivery assignment"));
    }

    @Transactional(readOnly = true)
    public List<DeliveryAssignment> listDeliveries() {
        return assignments.findAll();
    }

    // --- Driver Operations ---

    @Transactional
    public Driver createDriver(Requests.Driver request) {
        Driver d = new Driver();
        d.setDriverCode(request.driverCode());
        d.setName(request.name());
        d.setPhone(request.phone());
        d.setLicenseNumber(request.licenseNumber());
        return drivers.save(d);
    }

    @Transactional(readOnly = true)
    public Driver getDriver(Long id) {
        return drivers.findById(id).orElseThrow(() -> notFound("Driver"));
    }

    @Transactional
    public Driver updateDriver(Long id, Requests.UpdateDriver request) {
        Driver d = getDriver(id);
        d.setName(request.name());
        d.setPhone(request.phone());
        return drivers.save(d);
    }

    @Transactional
    public Driver updateDriverStatus(Long id, DriverStatus status) {
        Driver d = getDriver(id);
        d.setStatus(status);
        return drivers.save(d);
    }

    @Transactional(readOnly = true)
    public List<Driver> drivers() {
        return drivers.findAll();
    }

    // --- Vehicle Operations ---

    @Transactional
    public Vehicle createVehicle(Requests.Vehicle request) {
        Vehicle v = new Vehicle();
        v.setVehicleNumber(request.vehicleNumber());
        v.setVehicleType(request.vehicleType());
        v.setCapacityKg(request.capacityKg());
        return vehicles.save(v);
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicle(Long id) {
        return vehicles.findById(id).orElseThrow(() -> notFound("Vehicle"));
    }

    @Transactional
    public Vehicle updateVehicle(Long id, Requests.UpdateVehicle request) {
        Vehicle v = getVehicle(id);
        v.setVehicleType(request.vehicleType());
        v.setCapacityKg(request.capacityKg());
        return vehicles.save(v);
    }

    @Transactional
    public Vehicle updateVehicleStatus(Long id, VehicleStatus status) {
        Vehicle v = getVehicle(id);
        v.setStatus(status);
        return vehicles.save(v);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> vehicles() {
        return vehicles.findAll();
    }

    // --- Warehouse Operations ---

    @Transactional
    public Warehouse createWarehouse(Requests.Warehouse request) {
        Warehouse w = new Warehouse();
        w.setWarehouseCode(request.warehouseCode());
        w.setName(request.name());
        w.setAddress(request.address());
        w.setCity(request.city());
        w.setState(request.state());
        w.setCapacity(request.capacity());
        w.setStatus(request.status());
        return warehouses.save(w);
    }

    @Transactional(readOnly = true)
    public Warehouse getWarehouse(Long id) {
        return warehouses.findById(id).orElseThrow(() -> notFound("Warehouse"));
    }

    @Transactional
    public Warehouse updateWarehouse(Long id, Requests.UpdateWarehouse request) {
        Warehouse w = getWarehouse(id);
        w.setName(request.name());
        w.setAddress(request.address());
        w.setCity(request.city());
        w.setState(request.state());
        w.setCapacity(request.capacity());
        w.setStatus(request.status());
        return warehouses.save(w);
    }

    @Transactional(readOnly = true)
    public List<Warehouse> warehouses() {
        return warehouses.findAll();
    }

    // --- Webhook Subscription Operations ---

    @Transactional(readOnly = true)
    public List<WebhookSubscription> listWebhookSubscriptions() {
        return webhookSubscriptions.findAll();
    }

    @Transactional
    public WebhookSubscription createWebhookSubscription(Requests.WebhookSubscription request) {
        WebhookSubscription sub = new WebhookSubscription();
        sub.setTargetUrl(request.targetUrl());
        sub.setSecret(request.secret());
        if (request.customerId() != null) {
            sub.setCustomer(customers.findById(request.customerId()).orElseThrow(() -> notFound("Customer")));
        }
        return webhookSubscriptions.save(sub);
    }

    @Transactional
    public void deleteWebhookSubscription(Long id) {
        if (!webhookSubscriptions.existsById(id)) {
            throw notFound("Webhook subscription");
        }
        webhookSubscriptions.deleteById(id);
    }

    // --- Private Helper Methods ---

    private Address toAddress(Requests.Address r) {
        Address a = new Address();
        a.setAddressLine1(r.addressLine1());
        a.setAddressLine2(r.addressLine2());
        a.setCity(r.city());
        a.setState(r.state());
        a.setPostalCode(r.postalCode());
        a.setCountry(r.country());
        return a;
    }

    private void updateAddress(Address target, Requests.Address r) {
        target.setAddressLine1(r.addressLine1());
        target.setAddressLine2(r.addressLine2());
        target.setCity(r.city());
        target.setState(r.state());
        target.setPostalCode(r.postalCode());
        target.setCountry(r.country());
    }

    private PackageItem toPackage(LogisticsOrder order, Requests.Package r) {
        PackageItem item = new PackageItem();
        item.setPackageCode(code("PKG"));
        item.setOrder(order);
        item.setDescription(r.description());
        item.setWeightKg(r.weightKg());
        item.setLengthCm(r.lengthCm());
        item.setWidthCm(r.widthCm());
        item.setHeightCm(r.heightCm());
        item.setDeclaredValue(r.declaredValue());
        return item;
    }

    private void recordEvent(Shipment s, String type, String description, String location) {
        TrackingEvent e = new TrackingEvent();
        e.setShipment(s);
        e.setEventType(type);
        e.setStatus(s.getStatus().name());
        e.setDescription(description);
        e.setLocation(location);
        events.save(e);
    }

    private boolean allowed(ShipmentStatus from, ShipmentStatus to) {
        return switch (from) {
            case CREATED -> to == ShipmentStatus.PICKED_UP || to == ShipmentStatus.CANCELLED;
            case PICKED_UP -> to == ShipmentStatus.IN_TRANSIT || to == ShipmentStatus.FAILED;
            case IN_TRANSIT -> to == ShipmentStatus.AT_WAREHOUSE || to == ShipmentStatus.FAILED;
            case AT_WAREHOUSE -> to == ShipmentStatus.OUT_FOR_DELIVERY || to == ShipmentStatus.FAILED;
            case OUT_FOR_DELIVERY -> to == ShipmentStatus.DELIVERED || to == ShipmentStatus.FAILED || to == ShipmentStatus.AT_WAREHOUSE;
            case FAILED -> to == ShipmentStatus.OUT_FOR_DELIVERY;
            default -> false;
        };
    }

    private boolean validDeliveryTransition(DeliveryStatus from, DeliveryStatus to) {
        return switch (from) {
            case ASSIGNED -> to == DeliveryStatus.PICKED_UP || to == DeliveryStatus.CANCELLED;
            case PICKED_UP -> to == DeliveryStatus.OUT_FOR_DELIVERY || to == DeliveryStatus.FAILED;
            case OUT_FOR_DELIVERY -> to == DeliveryStatus.DELIVERED || to == DeliveryStatus.FAILED;
            default -> false;
        };
    }

    private String code(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private ApiException notFound(String type) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", type + " not found");
    }

    private ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "BUSINESS_RULE_VIOLATION", message);
    }
}
