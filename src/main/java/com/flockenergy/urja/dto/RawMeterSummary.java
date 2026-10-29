package com.flockenergy.urja.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawMeterSummary(
        String meterId,
        String serialNo,
        String make,
        String phaseType,
        String installStatus,
        String dtCode
) {}
