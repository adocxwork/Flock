package com.flockenergy.urja.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawEnergyRecord(
        String timestamp,
        String kwh,
        String kvah,
        String voltR
) {
    public Double parseKwh() {
        if (kwh == null || kwh.isBlank() || kwh.equals("—")) return null;
        try {
            return Double.parseDouble(kwh);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double parseKvah() {
        if (kvah == null || kvah.isBlank() || kvah.equals("—")) return null;
        try {
            return Double.parseDouble(kvah);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double parseVoltR() {
        if (voltR == null || voltR.isBlank() || voltR.equals("—")) return null;
        try {
            return Double.parseDouble(voltR);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
