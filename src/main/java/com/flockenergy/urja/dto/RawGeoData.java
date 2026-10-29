package com.flockenergy.urja.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawGeoData(
        String latitude,
        String longitude
) {
    public Double getLatitudeAsDouble() {
        if (latitude == null || latitude.isBlank()) return null;
        try {
            return Double.parseDouble(latitude);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double getLongitudeAsDouble() {
        if (longitude == null || longitude.isBlank()) return null;
        try {
            return Double.parseDouble(longitude);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
