package com.flockenergy.urja.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Meter(
        String meterId,
        String serialNo,
        String make,
        String phaseType,
        String installStatus,
        String installType,
        String build,
        String dtCode,
        GeoLocation geo,
        MeterHierarchy hierarchy
) {}
