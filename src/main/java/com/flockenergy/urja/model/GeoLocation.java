package com.flockenergy.urja.model;

public record GeoLocation(
        Double lat,
        Double lng
) {
    public boolean isValid() {
        return lat != null && lng != null &&
               lat >= -90.0 && lat <= 90.0 &&
               lng >= -180.0 && lng <= 180.0;
    }

    /**
     * Calculates distance to another coordinate using the Haversine formula in kilometers.
     */
    public double distanceToKm(double targetLat, double targetLng) {
        if (!isValid()) return Double.MAX_VALUE;
        double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(targetLat - this.lat);
        double dLng = Math.toRadians(targetLng - this.lng);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(this.lat)) * Math.cos(Math.toRadians(targetLat)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
