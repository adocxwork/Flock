package com.flockenergy.urja;

import com.flockenergy.urja.model.GeoLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoLocationTest {

    @Test
    void testValidGeoCoordinates() {
        GeoLocation valid = new GeoLocation(26.9389, 75.8309);
        assertTrue(valid.isValid());

        GeoLocation invalidLat = new GeoLocation(95.0, 75.0);
        assertFalse(invalidLat.isValid());

        GeoLocation nullCoord = new GeoLocation(null, 75.0);
        assertFalse(nullCoord.isValid());
    }

    @Test
    void testDistanceCalculation() {
        // Jaipur center to nearby point ~ 13 km away
        GeoLocation p1 = new GeoLocation(26.9389, 75.8309);
        GeoLocation p2 = new GeoLocation(26.8221, 75.9071);

        double dist = p1.distanceToKm(p2.lat(), p2.lng());
        assertTrue(dist > 10.0 && dist < 20.0, "Distance should be approx 15 km");

        // Same point distance must be 0
        assertEquals(0.0, p1.distanceToKm(p1.lat(), p1.lng()), 0.001);
    }
}
