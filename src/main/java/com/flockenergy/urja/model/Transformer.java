package com.flockenergy.urja.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Transformer(
        String code,
        String name,
        String feederCode,
        int capacityKva,
        int meterCount,
        int installedMeters,
        int faultyMeters,
        int decommissionedMeters,
        String status
) {}
