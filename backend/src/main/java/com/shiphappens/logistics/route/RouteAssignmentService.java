package com.shiphappens.logistics.route;

import com.shiphappens.logistics.entity.Shipment;
import com.shiphappens.logistics.entity.Vehicle;
import com.shiphappens.logistics.repository.PackageRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class RouteAssignmentService {

    private final PackageRepository packageRepository;

    public RouteAssignmentService(PackageRepository packageRepository) {
        this.packageRepository = packageRepository;
    }

    public record UnassignedShipment(Shipment shipment, String reason) {}

    public record AssignmentResult(
            Map<Vehicle, List<Shipment>> vehicleAssignments,
            List<UnassignedShipment> unassignedShipments
    ) {}

    public AssignmentResult assignShipments(List<Shipment> shipments, List<Vehicle> vehicles) {
        Map<Vehicle, List<Shipment>> vehicleAssignments = new LinkedHashMap<>();
        List<UnassignedShipment> unassigned = new ArrayList<>();

        if (shipments == null || shipments.isEmpty()) {
            return new AssignmentResult(vehicleAssignments, unassigned);
        }

        if (vehicles == null || vehicles.isEmpty()) {
            for (Shipment s : shipments) {
                unassigned.add(new UnassignedShipment(s, "NO_AVAILABLE_VEHICLES"));
            }
            return new AssignmentResult(vehicleAssignments, unassigned);
        }

        // Initialize empty lists and track remaining capacities
        Map<Long, BigDecimal> remainingCapacity = new HashMap<>();
        for (Vehicle v : vehicles) {
            vehicleAssignments.put(v, new ArrayList<>());
            remainingCapacity.put(v.getId(), v.getCapacityKg() != null ? v.getCapacityKg() : BigDecimal.ZERO);
        }

        // Sort shipments descending by weight (First-Fit Decreasing)
        List<Shipment> sortedShipments = new ArrayList<>(shipments);
        sortedShipments.sort((s1, s2) -> getWeight(s2).compareTo(getWeight(s1)));

        for (Shipment shipment : sortedShipments) {
            BigDecimal weight = getWeight(shipment);
            boolean assigned = false;

            for (Vehicle vehicle : vehicles) {
                BigDecimal remaining = remainingCapacity.get(vehicle.getId());
                if (remaining.compareTo(weight) >= 0) {
                    vehicleAssignments.get(vehicle).add(shipment);
                    remainingCapacity.put(vehicle.getId(), remaining.subtract(weight));
                    assigned = true;
                    break;
                }
            }

            if (!assigned) {
                unassigned.add(new UnassignedShipment(shipment, "NO_VEHICLE_CAPACITY"));
            }
        }

        // Filter out vehicles that received 0 shipments
        Map<Vehicle, List<Shipment>> activeAssignments = new LinkedHashMap<>();
        for (Map.Entry<Vehicle, List<Shipment>> entry : vehicleAssignments.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                activeAssignments.put(entry.getKey(), entry.getValue());
            }
        }

        return new AssignmentResult(activeAssignments, unassigned);
    }

    public BigDecimal getWeight(Shipment shipment) {
        if (shipment.getWeightKg() != null && shipment.getWeightKg().compareTo(BigDecimal.ZERO) > 0) {
            return shipment.getWeightKg();
        }
        if (shipment.getOrder() != null) {
            BigDecimal total = packageRepository.totalWeight(shipment.getOrder());
            if (total != null && total.compareTo(BigDecimal.ZERO) > 0) {
                return total;
            }
        }
        return BigDecimal.ONE; // default fallback weight
    }
}
