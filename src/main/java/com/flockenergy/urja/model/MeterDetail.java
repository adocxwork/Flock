package com.flockenergy.urja.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MeterDetail(
        Meter meter,
        ConsumptionSummary consumptionSummary,
        List<ConsumptionReading> readings
) {}
