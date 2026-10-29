package com.flockenergy.urja.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawMeterSearchResponse(
        List<RawMeterSummary> data,
        int total,
        int page,
        int pageSize
) {}
