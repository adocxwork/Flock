package com.flockenergy.urja.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawDtsResponse(
        List<RawDtRecord> data,
        int total,
        int page,
        int pageSize
) {}
