package com.flockenergy.urja.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConsumptionReading(
        String timestamp,
        String isoTimestamp,
        Double kwh,
        Double kvah,
        Double voltR
) {
    private static final DateTimeFormatter PORTAL_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static ConsumptionReading fromRaw(String rawTimestamp, Double kwh, Double kvah, Double voltR) {
        String iso = null;
        if (rawTimestamp != null && !rawTimestamp.isBlank()) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(rawTimestamp.trim(), PORTAL_FORMAT);
                iso = ldt.format(ISO_FORMAT);
            } catch (Exception ignored) {
                iso = rawTimestamp;
            }
        }
        return new ConsumptionReading(rawTimestamp, iso, kwh, kvah, voltR);
    }
}
