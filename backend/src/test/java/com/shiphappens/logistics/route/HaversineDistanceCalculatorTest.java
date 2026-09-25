package com.shiphappens.logistics.route;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HaversineDistanceCalculatorTest {

    private final HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    @DisplayName("Distance between identical coordinates is 0.0 km")
    void testZeroDistance() {
        double dist = calculator.calculateKm(17.385044, 78.486671, 17.385044, 78.486671);
        assertEquals(0.0, dist, 0.001);
    }

    @Test
    @DisplayName("Calculates realistic geographic distance between Hyderabad and Bengaluru")
    void testCityDistance() {
        // Hyderabad (17.3850, 78.4867) to Bengaluru (12.9716, 77.5946) ~500 km
        double dist = calculator.calculateKm(17.385044, 78.486671, 12.971599, 77.594566);
        assertTrue(dist > 480.0 && dist < 520.0, "Expected ~500 km, got: " + dist);
    }

    @Test
    @DisplayName("Calculates intra-city distance accurately")
    void testShortDistance() {
        // Depot to Banjara Hills ~5.5 km
        double dist = calculator.calculateKm(17.385044, 78.486671, 17.415563, 78.435776);
        assertTrue(dist > 4.0 && dist < 8.0, "Expected ~5-6 km, got: " + dist);
    }
}
