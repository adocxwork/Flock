package com.flockenergy.urja.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawDtRecord(
        String code,
        String name,
        String feederCode,
        int capacityKva
) {}
