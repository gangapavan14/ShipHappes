package com.shiphappens.logistics.route;

import com.shiphappens.logistics.entity.Shipment;
import com.shiphappens.logistics.entity.Vehicle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class RouteSequencingService {

    private static final double DEFAULT_LATITUDE = 17.385044;
    private static final double DEFAULT_LONGITUDE = 78.486671;

    private final RouteSequencer sequencer;
    private final DistanceCalculator distanceCalculator;
    private final TwoOptRouteOptimizer twoOptOptimizer;

    @org.springframework.beans.factory.annotation.Autowired
    public RouteSequencingService(RouteSequencer sequencer, DistanceCalculator distanceCalculator, TwoOptRouteOptimizer twoOptOptimizer) {
        this.sequencer = sequencer;
        this.distanceCalculator = distanceCalculator;
        this.twoOptOptimizer = twoOptOptimizer;
    }

    public RouteSequencingService(RouteSequencer sequencer, DistanceCalculator distanceCalculator) {
        this(sequencer, distanceCalculator, new TwoOptRouteOptimizer(distanceCalculator));
    }

    public record SequencedStop(
            Shipment shipment,
            int sequenceOrder,
            BigDecimal distanceFromPreviousKm,
            Double arrivalLatitude,
            Double arrivalLongitude
    ) {}

    public record SequencedRoute(
            List<SequencedStop> stops,
            BigDecimal totalDistanceKm,
            BigDecimal initialDistanceKm,
            BigDecimal distanceSavedKm,
            BigDecimal savingsPercentage,
            BigDecimal co2SavedKg
    ) {
        public SequencedRoute(List<SequencedStop> stops, BigDecimal totalDistanceKm) {
            this(stops, totalDistanceKm, totalDistanceKm, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    public SequencedRoute planSequence(Vehicle vehicle, List<Shipment> shipments) {
        if (shipments == null || shipments.isEmpty()) {
            return new SequencedRoute(List.of(), BigDecimal.ZERO);
        }

        // 1. Initial nearest-neighbor heuristic
        List<Shipment> initialOrdered = sequencer.sequence(vehicle, shipments);
        double initialDistance = calculateDistance(vehicle, initialOrdered);

        // 2. 2-Opt local search refinement
        List<Shipment> optimizedOrdered = (twoOptOptimizer != null)
                ? twoOptOptimizer.optimize(vehicle, initialOrdered)
                : initialOrdered;

        List<SequencedStop> stops = new ArrayList<>(optimizedOrdered.size());
        double currentLat = (vehicle != null && vehicle.getStartLatitude() != null)
                ? vehicle.getStartLatitude() : DEFAULT_LATITUDE;
        double currentLon = (vehicle != null && vehicle.getStartLongitude() != null)
                ? vehicle.getStartLongitude() : DEFAULT_LONGITUDE;

        double finalDistance = 0.0;
        int order = 1;

        for (Shipment s : optimizedOrdered) {
            double destLat = s.getEffectiveLatitude() != null ? s.getEffectiveLatitude() : DEFAULT_LATITUDE;
            double destLon = s.getEffectiveLongitude() != null ? s.getEffectiveLongitude() : DEFAULT_LONGITUDE;

            double dist = distanceCalculator.calculateKm(currentLat, currentLon, destLat, destLon);
            finalDistance += dist;

            stops.add(new SequencedStop(
                    s,
                    order++,
                    BigDecimal.valueOf(dist).setScale(2, RoundingMode.HALF_UP),
                    destLat,
                    destLon
            ));

            currentLat = destLat;
            currentLon = destLon;
        }

        double savedKm = Math.max(0.0, initialDistance - finalDistance);
        double pct = (initialDistance > 0.001) ? (savedKm / initialDistance) * 100.0 : 0.0;
        double co2Kg = savedKm * 0.15; // Average 0.15 kg CO2 per km saved

        return new SequencedRoute(
                stops,
                BigDecimal.valueOf(finalDistance).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(initialDistance).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(savedKm).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(pct).setScale(1, RoundingMode.HALF_UP),
                BigDecimal.valueOf(co2Kg).setScale(2, RoundingMode.HALF_UP)
        );
    }

    private double calculateDistance(Vehicle vehicle, List<Shipment> tour) {
        double currentLat = (vehicle != null && vehicle.getStartLatitude() != null)
                ? vehicle.getStartLatitude() : DEFAULT_LATITUDE;
        double currentLon = (vehicle != null && vehicle.getStartLongitude() != null)
                ? vehicle.getStartLongitude() : DEFAULT_LONGITUDE;
        double total = 0.0;
        for (Shipment s : tour) {
            double destLat = s.getEffectiveLatitude() != null ? s.getEffectiveLatitude() : DEFAULT_LATITUDE;
            double destLon = s.getEffectiveLongitude() != null ? s.getEffectiveLongitude() : DEFAULT_LONGITUDE;
            total += distanceCalculator.calculateKm(currentLat, currentLon, destLat, destLon);
            currentLat = destLat;
            currentLon = destLon;
        }
        return total;
    }
}
