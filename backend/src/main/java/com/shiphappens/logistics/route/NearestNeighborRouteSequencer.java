package com.shiphappens.logistics.route;

import com.shiphappens.logistics.entity.Shipment;
import com.shiphappens.logistics.entity.Vehicle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NearestNeighborRouteSequencer implements RouteSequencer {

    private static final double DEFAULT_LATITUDE = 17.385044;
    private static final double DEFAULT_LONGITUDE = 78.486671;

    private final DistanceCalculator distanceCalculator;

    public NearestNeighborRouteSequencer(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    @Override
    public List<Shipment> sequence(Vehicle vehicle, List<Shipment> shipments) {
        if (shipments == null || shipments.isEmpty()) {
            return List.of();
        }

        List<Shipment> remaining = new ArrayList<>(shipments);
        List<Shipment> sequenced = new ArrayList<>(shipments.size());

        double currentLat = (vehicle != null && vehicle.getStartLatitude() != null)
                ? vehicle.getStartLatitude()
                : DEFAULT_LATITUDE;
        double currentLon = (vehicle != null && vehicle.getStartLongitude() != null)
                ? vehicle.getStartLongitude()
                : DEFAULT_LONGITUDE;

        while (!remaining.isEmpty()) {
            Shipment nearest = null;
            double minDistance = Double.MAX_VALUE;
            int nearestIndex = -1;

            for (int i = 0; i < remaining.size(); i++) {
                Shipment s = remaining.get(i);
                double destLat = s.getEffectiveLatitude() != null ? s.getEffectiveLatitude() : DEFAULT_LATITUDE;
                double destLon = s.getEffectiveLongitude() != null ? s.getEffectiveLongitude() : DEFAULT_LONGITUDE;

                double dist = distanceCalculator.calculateKm(currentLat, currentLon, destLat, destLon);
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = s;
                    nearestIndex = i;
                }
            }

            if (nearest != null && nearestIndex >= 0) {
                sequenced.add(nearest);
                remaining.remove(nearestIndex);
                currentLat = nearest.getEffectiveLatitude() != null ? nearest.getEffectiveLatitude() : DEFAULT_LATITUDE;
                currentLon = nearest.getEffectiveLongitude() != null ? nearest.getEffectiveLongitude() : DEFAULT_LONGITUDE;
            } else {
                // Fallback: take first remaining
                sequenced.add(remaining.remove(0));
            }
        }

        return sequenced;
    }
}
