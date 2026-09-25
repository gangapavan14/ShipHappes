package com.shiphappens.logistics.route;

import org.springframework.stereotype.Component;

@Component
public class HaversineDistanceCalculator implements DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public double calculateKm(double fromLat, double fromLon, double toLat, double toLon) {
        if (Double.compare(fromLat, toLat) == 0 && Double.compare(fromLon, toLon) == 0) {
            return 0.0;
        }

        double dLat = Math.toRadians(toLat - fromLat);
        double dLon = Math.toRadians(toLon - fromLon);

        double radFromLat = Math.toRadians(fromLat);
        double radToLat = Math.toRadians(toLat);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(radFromLat) * Math.cos(radToLat);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS_KM * c;
        // Round to 2 decimal places for stable values
        return Math.round(distance * 100.0) / 100.0;
    }
}
