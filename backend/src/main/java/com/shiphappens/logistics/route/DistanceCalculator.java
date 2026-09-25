package com.shiphappens.logistics.route;

public interface DistanceCalculator {
    /**
     * Calculates distance in kilometers between two geographic coordinates.
     */
    double calculateKm(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude);
}
