package com.flockenergy.urja.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalyticsOverview(
        int totalMeters,
        int installedMeters,
        int faultyMeters,
        int decommissionedMeters,
        double healthScorePercentage,
        int totalTransformers,
        int totalCapacityKva,
        Map<String, Long> makeDistribution,
        Map<String, Long> phaseDistribution,
        Map<String, Long> statusDistribution,
        Map<String, Long> buildDistribution
) {}
