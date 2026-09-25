package com.shiphappens.logistics.route;

import com.shiphappens.logistics.entity.Shipment;
import com.shiphappens.logistics.entity.Vehicle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 2-Opt Local Search Heuristic for the Traveling Salesperson Problem (TSP).
 * Iteratively untangles crossing paths in the nearest-neighbor tour until no
 * 2-edge swap can further reduce the total Euclidean/Haversine tour distance.
 */
@Component
public class TwoOptRouteOptimizer {

    private final DistanceCalculator distanceCalculator;

    public TwoOptRouteOptimizer(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    public List<Shipment> optimize(Vehicle vehicle, List<Shipment> initialTour) {
        if (initialTour == null || initialTour.size() <= 2) {
            return initialTour != null ? new ArrayList<>(initialTour) : List.of();
        }

        List<Shipment> tour = new ArrayList<>(initialTour);
        double depotLat = (vehicle != null && vehicle.getStartLatitude() != null)
                ? vehicle.getStartLatitude() : 17.385044;
        double depotLon = (vehicle != null && vehicle.getStartLongitude() != null)
                ? vehicle.getStartLongitude() : 78.486671;

        boolean improved = true;
        int maxIterations = 50; // Safety threshold for convergence
        int iteration = 0;

        while (improved && iteration++ < maxIterations) {
            improved = false;
            int n = tour.size();

            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    double delta = computeSwapDelta(tour, i, j, depotLat, depotLon);
                    if (delta < -1e-4) { // Significant distance reduction
                        reverseSubTour(tour, i, j);
                        improved = true;
                        break;
                    }
                }
                if (improved) break;
            }
        }

        return tour;
    }

    private double computeSwapDelta(List<Shipment> tour, int i, int j, double depotLat, double depotLon) {
        Shipment prevI = (i == 0) ? null : tour.get(i - 1);
        Shipment currentI = tour.get(i);
        Shipment currentJ = tour.get(j);
        Shipment nextJ = (j == tour.size() - 1) ? null : tour.get(j + 1);

        double pLat = getLat(prevI, depotLat);
        double pLon = getLon(prevI, depotLon);

        double iLat = getLat(currentI, depotLat);
        double iLon = getLon(currentI, depotLon);

        double jLat = getLat(currentJ, depotLat);
        double jLon = getLon(currentJ, depotLon);

        double nextLat = getLat(nextJ, depotLat);
        double nextLon = getLon(nextJ, depotLon);

        // Current distance of edges (prevI -> currentI) and (currentJ -> nextJ)
        double currentDist = distanceCalculator.calculateKm(pLat, pLon, iLat, iLon);
        if (nextJ != null) {
            currentDist += distanceCalculator.calculateKm(jLat, jLon, nextLat, nextLon);
        }

        // New distance if we reconnect (prevI -> currentJ) and (currentI -> nextJ)
        double newDist = distanceCalculator.calculateKm(pLat, pLon, jLat, jLon);
        if (nextJ != null) {
            newDist += distanceCalculator.calculateKm(iLat, iLon, nextLat, nextLon);
        }

        return newDist - currentDist;
    }

    private double getLat(Shipment s, double fallbackLat) {
        if (s == null) return fallbackLat;
        Double lat = s.getEffectiveLatitude();
        return lat != null ? lat : fallbackLat;
    }

    private double getLon(Shipment s, double fallbackLon) {
        if (s == null) return fallbackLon;
        Double lon = s.getEffectiveLongitude();
        return lon != null ? lon : fallbackLon;
    }

    private void reverseSubTour(List<Shipment> tour, int i, int j) {
        while (i < j) {
            Shipment temp = tour.get(i);
            tour.set(i, tour.get(j));
            tour.set(j, temp);
            i++;
            j--;
        }
    }
}
