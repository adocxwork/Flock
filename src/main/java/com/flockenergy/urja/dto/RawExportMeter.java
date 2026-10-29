package com.flockenergy.urja.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawExportMeter(
        String meterId,
        String serialNo,
        String make,
        String phaseType,
        String installStatus,
        String installType,
        String build,
        String dtCode,
        RawMeterHierarchy hierarchy,
        RawGeoCoordinates geo
) {}
